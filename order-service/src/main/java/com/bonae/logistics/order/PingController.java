package com.bonae.logistics.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/api/orders/ping")
    public String ping() {
        return "order-service OK";
    }
}