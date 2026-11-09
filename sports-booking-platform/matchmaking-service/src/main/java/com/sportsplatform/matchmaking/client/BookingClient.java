package com.sportsplatform.matchmaking.client;

import com.sportsplatform.matchmaking.dto.BookingInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "booking-service", url = "${services.booking-url}")
public interface BookingClient {

    @GetMapping("/api/v1/internal/bookings/{id}")
    BookingInfoDto getBookingById(@PathVariable("id") UUID id);
}