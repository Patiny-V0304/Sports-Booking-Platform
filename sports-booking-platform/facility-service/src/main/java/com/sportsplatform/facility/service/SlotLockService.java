package com.sportsplatform.facility.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlotLockService {

    private final StringRedisTemplate redisTemplate;
    private static final String LOCK_PREFIX = "slot_lock:";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5); // Giữ chỗ đúng 5 phút

    public boolean tryLockSlot(UUID slotId, UUID userId) {
        String key = LOCK_PREFIX + slotId;
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, userId.toString(), LOCK_TTL);
        return Boolean.TRUE.equals(success);
    }

    public boolean isSlotLockedInRedis(UUID slotId) {
        String key = LOCK_PREFIX + slotId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void unlockSlot(UUID slotId) {
        String key = LOCK_PREFIX + slotId;
        redisTemplate.delete(key);
    }
}