package com.bonae.logistics.company;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 인프라 헬스체크 전용 엔드포인트이므로 공개 API 문서에서는 숨긴다.
@Hidden
@RestController
public class PingController {

    @GetMapping("/api/companies/ping")
    public String ping() {
        return "company-service OK";
    }
}