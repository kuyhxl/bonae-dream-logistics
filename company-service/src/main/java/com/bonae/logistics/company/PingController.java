package com.bonae.logistics.company;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/api/companies/ping")
    public String ping() {
        return "company-service OK";
    }
}