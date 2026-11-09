package com.sportsplatform.matchmaking.client;

import com.sportsplatform.matchmaking.dto.FacilityReviewInternalRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "facility-service", url = "${services.facility-url}")
public interface FacilityClient {

    @PostMapping("/api/v1/internal/facilities/{id}/reviews")
    void submitFacilityReview(@PathVariable("id") UUID facilityId,
                              @RequestBody FacilityReviewInternalRequest request);
}