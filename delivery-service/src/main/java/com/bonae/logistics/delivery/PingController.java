package com.bonae.logistics.delivery;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/api/deliveries/ping")
    public String ping() {
        return "delivery-service OK";
    }
}