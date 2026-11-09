package com.sportsplatform.facility.service;

import com.sportsplatform.common.dto.SlotLockResponse;
import com.sportsplatform.facility.dto.*;
import com.sportsplatform.facility.entity.Court;
import com.sportsplatform.facility.entity.CourtSlot;
import com.sportsplatform.facility.entity.Facility;
import com.sportsplatform.facility.entity.FacilityReview;
import com.sportsplatform.facility.repository.CourtRepository;
import com.sportsplatform.facility.repository.CourtSlotRepository;
import com.sportsplatform.facility.repository.FacilityRepository;
import com.sportsplatform.facility.repository.FacilityReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final CourtRepository courtRepository;
    private final CourtSlotRepository courtSlotRepository;
    private final FacilityReviewRepository facilityReviewRepository;
    private final SlotLockService slotLockService;

    @Transactional
    public Facility registerFacility(UUID managerId, RegisterFacilityRequest req) {
        Facility facility = Facility.builder()
                .managerId(managerId)
                .name(req.name())
                .address(req.address())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .businessLicenseUrl(req.businessLicenseUrl())
                .isOutdoor(req.isOutdoor())
                .status("PENDING_APPROVAL")
                .build();
        return facilityRepository.save(facility);
    }

    @Transactional
    public Facility approveOrRejectFacility(UUID facilityId, String status) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cụm sân"));
        if (!"ACTIVE".equals(status) && !"REJECTED".equals(status)) {
            throw new IllegalArgumentException("Trạng thái duyệt chỉ nhận ACTIVE hoặc REJECTED");
        }
        facility.setStatus(status);
        return facilityRepository.save(facility);
    }

    public List<Facility> findNearbyFacilities(double lat, double lng, double radiusKm) {
        return facilityRepository.findNearbyActiveFacilities(lat, lng, radiusKm);
    }

    @Transactional
    public Court createCourtAndSlots(UUID managerId, UUID facilityId, CreateCourtWithSlotsRequest req) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cụm sân"));

        if (!facility.getManagerId().equals(managerId)) {
            throw new IllegalStateException("Bạn không phải Chủ sân của cụm sân này");
        }
        if (!"ACTIVE".equals(facility.getStatus())) {
            throw new IllegalStateException("Cụm sân chưa được Admin phê duyệt (ACTIVE)");
        }

        Court court = Court.builder()
                .facility(facility)
                .courtName(req.courtName())
                .sportType(req.sportType())
                .status("ACTIVE")
                .build();
        courtRepository.save(court);

        List<CourtSlot> slots = new ArrayList<>();
        for (CreateCourtWithSlotsRequest.SlotItem item : req.slots()) {
            slots.add(CourtSlot.builder()
                    .court(court)
                    .playDate(item.playDate())
                    .startTime(item.startTime())
                    .endTime(item.endTime())
                    .price(item.price())
                    .status("AVAILABLE")
                    .build());
        }
        courtSlotRepository.saveAll(slots);
        return court;
    }

    public List<SlotViewResponse> getFacilitySlotsByDate(UUID facilityId, LocalDate playDate) {
        List<CourtSlot> slots = courtSlotRepository
                .findByCourt_Facility_IdAndPlayDateOrderByStartTimeAsc(facilityId, playDate);

        return slots.stream().map(slot -> {
            // Nếu DB đang là AVAILABLE nhưng trên Redis đang có khóa 5 phút -> Hiển thị là LOCKED
            String effectiveStatus = slot.getStatus();
            if ("AVAILABLE".equals(effectiveStatus) && slotLockService.isSlotLockedInRedis(slot.getId())) {
                effectiveStatus = "LOCKED";
            }
            return new SlotViewResponse(
                    slot.getId(),
                    slot.getCourt().getId(),
                    slot.getCourt().getCourtName(),
                    slot.getCourt().getSportType(),
                    slot.getPlayDate(),
                    slot.getStartTime(),
                    slot.getEndTime(),
                    slot.getPrice(),
                    effectiveStatus
            );
        }).toList();
    }

    /**
     * API Nội bộ: Khóa tạm Slot trên Redis trong 5 phút khi Người chơi bấm "Đặt sân"
     */
    @Transactional
    public SlotLockResponse lockSlotForBooking(UUID slotId, UUID userId) {
        CourtSlot slot = courtSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khung giờ sân"));

        if (!"AVAILABLE".equals(slot.getStatus())) {
            throw new IllegalStateException("Khung giờ này đã có người đặt (BOOKED)");
        }

        // Thử khóa nguyên tử trên Redis (SETNX TTL 5 phút)
        boolean locked = slotLockService.tryLockSlot(slotId, userId);
        if (!locked) {
            throw new IllegalStateException("Khung giờ này đang được người chơi khác giữ chỗ thanh toán");
        }

        Court court = slot.getCourt();
        Facility facility = court.getFacility();

        return new SlotLockResponse(
                slot.getId(),
                court.getId(),
                facility.getId(),
                facility.getName(),
                court.getCourtName(),
                Boolean.TRUE.equals(facility.getIsOutdoor()),
                slot.getPlayDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getPrice()
        );
    }

    /**
     * API Nội bộ: Chốt trạng thái BOOKED hoặc trả về AVAILABLE (khi hủy đơn / đổi lịch)
     */
    @Transactional
    public void updateSlotStatus(UUID slotId, String status, UUID bookingId) {
        CourtSlot slot = courtSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khung giờ sân"));

        slot.setStatus(status);
        slot.setCurrentBookingId("BOOKED".equals(status) ? bookingId : null);
        courtSlotRepository.save(slot);

        // Xóa khóa tạm trên Redis sau khi đã chốt trạng thái DB hoặc khi giải phóng sân
        slotLockService.unlockSlot(slotId);
    }

    /**
     * API Nội bộ: Ghi nhận đánh giá 1-5 sao và tính lại điểm trung bình sao của cụm sân
     */
    @Transactional
    public void addFacilityReview(UUID facilityId, CreateFacilityReviewRequest req) {
        if (facilityReviewRepository.existsByBookingId(req.bookingId())) {
            throw new IllegalStateException("Đơn đặt sân này đã được đánh giá trước đó");
        }
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cụm sân"));

        FacilityReview review = FacilityReview.builder()
                .facility(facility)
                .bookingId(req.bookingId())
                .userId(req.userId())
                .starRating(req.starRating())
                .comment(req.comment())
                .build();
        facilityReviewRepository.save(review);

        Double avg = facilityReviewRepository.calculateAverageRating(facilityId);
        facility.setRatingAvg(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        facilityRepository.save(facility);
    }
}