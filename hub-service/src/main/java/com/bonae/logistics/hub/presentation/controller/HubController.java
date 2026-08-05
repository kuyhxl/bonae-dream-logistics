package com.bonae.logistics.hub.presentation.controller;

import com.bonae.logistics.hub.application.service.HubService;
import com.bonae.logistics.hub.presentation.dto.response.HubResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/hubs")
public class HubController {

    private final HubService hubService;

    @GetMapping("/{hubId}")
    public ResponseEntity<HubResponse> getHub(@PathVariable UUID hubId) {
        return ResponseEntity.ok(hubService.getHub(hubId));
    }
}
