package com.bonae.logistics.delivery.infrastructure.client;

import com.bonae.logistics.delivery.infrastructure.client.dto.HubRoutePathClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "hub-service")
public interface HubRouteClient {

    @GetMapping("/api/internal/hub-routes")
    HubRoutePathClientResponse getShortestPath(
            @RequestParam("departureHubId") UUID departureHubId,
            @RequestParam("arrivalHubId") UUID arrivalHubId
    );
}
