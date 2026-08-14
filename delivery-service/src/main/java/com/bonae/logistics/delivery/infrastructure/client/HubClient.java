package com.bonae.logistics.delivery.infrastructure.client;

import com.bonae.logistics.delivery.infrastructure.client.dto.HubSummaryClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "hub-service")
public interface HubClient {

    @GetMapping("/api/internal/hubs")
    List<HubSummaryClientResponse> getHubsByIds(@RequestParam("hubIds") List<UUID> hubIds);
}
