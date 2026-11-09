package com.sportsplatform.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "identity-service", url = "${services.identity-url}")
public interface IdentityClient {

    @PostMapping("/api/v1/internal/users/{userId}/reliability")
    void adjustReliability(@PathVariable("userId") UUID userId,
                           @RequestParam("delta") int delta,
                           @RequestParam("reason") String reason,
                           @RequestParam(value = "bookingId", required = false) UUID bookingId);
}