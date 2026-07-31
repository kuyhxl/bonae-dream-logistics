package com.bonae.logistics.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/api/users/ping")
    public String ping() {
        return "user-service OK";
    }
}