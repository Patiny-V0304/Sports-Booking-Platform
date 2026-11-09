package com.sportsplatform.identity.service;

import com.sportsplatform.identity.dto.*;
import com.sportsplatform.identity.entity.ReliabilityLog;
import com.sportsplatform.identity.entity.SportProfile;
import com.sportsplatform.identity.entity.User;
import com.sportsplatform.identity.repository.ReliabilityLogRepository;
import com.sportsplatform.identity.repository.SportProfileRepository;
import com.sportsplatform.identity.repository.UserRepository;
import com.sportsplatform.identity.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityService {

    private final UserRepository userRepository;
    private final SportProfileRepository sportProfileRepository;
    private final ReliabilityLogRepository reliabilityLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByPhone(req.phone())) {
            throw new IllegalArgumentException("Số điện thoại đã được đăng ký");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email đã được đăng ký");
        }

        User user = User.builder()
                .phone(req.phone())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .role(req.role())
                .reliabilityScore(100)
                .status("ACTIVE")
                .build();

        userRepository.save(user);
        String token = jwtUtil.generateToken(user);

        return new AuthResponse(token, user.getId(), user.getFullName(), user.getRole(), user.getReliabilityScore());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByPhoneOrEmail(req.phoneOrEmail(), req.phoneOrEmail())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác");
        }

        if ("BANNED".equals(user.getStatus())) {
            throw new IllegalStateException("Tài khoản của bạn đã bị khóa do vi phạm quy định");
        }

        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getFullName(), user.getRole(), user.getReliabilityScore());
    }

    public Map<String, Object> getMyProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        List<SportProfile> sports = sportProfileRepository.findByUserId(userId);

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("id", user.getId());
        profileData.put("phone", user.getPhone());
        profileData.put("email", user.getEmail());
        profileData.put("fullName", user.getFullName());
        profileData.put("role", user.getRole());
        profileData.put("reliabilityScore", user.getReliabilityScore());
        profileData.put("bankName", user.getBankName());
        profileData.put("bankAccountNo", user.getBankAccountNo());
        profileData.put("vietqrImageUrl", user.getVietqrImageUrl());
        profileData.put("sportProfiles", sports);
        return profileData;
    }

    @Transactional
    public User updateVietQr(UUID userId, UpdateVietQrRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        user.setBankName(req.bankName());
        user.setBankAccountNo(req.bankAccountNo());
        user.setVietqrImageUrl(req.vietqrImageUrl());
        return userRepository.save(user);
    }

    @Transactional
    public SportProfile updateSportProfile(UUID userId, UpdateSportProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        SportProfile profile = sportProfileRepository
                .findByUserIdAndSportType(userId, req.sportType())
                .orElse(SportProfile.builder()
                        .user(user)
                        .sportType(req.sportType())
                        .totalEvaluations(0)
                        .build());

        profile.setSkillLevel(req.skillLevel());
        return sportProfileRepository.save(profile);
    }

    // API nội bộ để Booking Service / Matchmaking Service gọi sang khi trừ điểm uy tín (No-show)
    @Transactional
    public void adjustReliabilityScore(UUID userId, int delta, String reason, UUID bookingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        int newScore = Math.max(0, Math.min(100, user.getReliabilityScore() + delta));
        user.setReliabilityScore(newScore);
        userRepository.save(user);

        ReliabilityLog log = ReliabilityLog.builder()
                .user(user)
                .changeAmount(delta)
                .reason(reason)
                .referenceBookingId(bookingId)
                .build();
        reliabilityLogRepository.save(log);
    }
}