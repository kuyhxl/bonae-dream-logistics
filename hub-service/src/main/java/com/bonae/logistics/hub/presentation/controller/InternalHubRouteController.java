package com.bonae.logistics.hub.presentation.controller;

import com.bonae.logistics.hub.application.service.HubRoutePathService;
import com.bonae.logistics.hub.presentation.dto.response.HubRoutePathResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/hub-routes")
public class InternalHubRouteController {

    private final HubRoutePathService hubRoutePathService;

    @GetMapping
    public ResponseEntity<HubRoutePathResponse> getShortestPath(@RequestParam UUID departureHubId, @RequestParam UUID arrivalHubId) {
        return ResponseEntity.ok(hubRoutePathService.findShortestPath(departureHubId, arrivalHubId));
    }
}
