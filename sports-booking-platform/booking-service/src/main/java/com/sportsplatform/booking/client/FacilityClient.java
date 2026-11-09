package com.sportsplatform.booking.client;

import com.sportsplatform.common.dto.SlotLockResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "facility-service", url = "${services.facility-url}")
public interface FacilityClient {

    @PostMapping("/api/v1/internal/slots/{slotId}/lock")
    SlotLockResponse lockSlot(@PathVariable("slotId") UUID slotId,
                              @RequestParam("userId") UUID userId);

    @PutMapping("/api/v1/internal/slots/{slotId}/status")
    void updateSlotStatus(@PathVariable("slotId") UUID slotId,
                          @RequestParam("status") String status,
                          @RequestParam(value = "bookingId", required = false) UUID bookingId);
}