package com.bonae.logistics.company.infrastructure;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "hub-service")
public interface HubClient {

    @GetMapping("/api/internal/hubs/{hubId}")
    ResponseEntity<Void> getHub(@PathVariable("hubId") UUID hubId);
}
