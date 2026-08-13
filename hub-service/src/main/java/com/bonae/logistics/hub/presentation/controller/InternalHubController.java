package com.bonae.logistics.hub.presentation.controller;

import com.bonae.logistics.hub.application.service.HubService;
import com.bonae.logistics.hub.presentation.dto.response.HubResponse;
import com.bonae.logistics.hub.presentation.dto.response.HubSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/hubs")
public class InternalHubController {

    private final HubService hubService;

    @GetMapping
    public ResponseEntity<List<HubSummaryResponse>> getHubsByIds(@RequestParam List<UUID> hubIds) {
        return ResponseEntity.ok(hubService.getHubsByIds(hubIds));
    }

    @GetMapping("/{hubId}")
    public ResponseEntity<HubResponse> validateHubExists(@PathVariable UUID hubId) {
        return ResponseEntity.ok(hubService.getHub(hubId));
    }
}
