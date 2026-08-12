package com.bonae.logistics.user.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "company-service")
public interface CompanyClient {

    // 없거나 삭제된 업체면 COMPANY_NOT_FOUND(404).
    // 응답의 hubId는 허브 관리자의 승인 범위(자기 허브 소속 업체인지) 검증에 쓴다.
    @GetMapping("/api/internal/companies/{companyId}")
    CompanyInfoDto getCompany(@PathVariable("companyId") UUID companyId);
}