package com.bonae.logistics.hub;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/api/hubs/ping")
    public String ping() {
        return "hub-service OK";
    }
}