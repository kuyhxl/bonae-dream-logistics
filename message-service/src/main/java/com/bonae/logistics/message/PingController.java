package com.bonae.logistics.message;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/api/slack-messages/ping")
    public String ping() {
        return "message-service OK";
    }
}