package com.bonae.logistics.user.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "company-service")
public interface CompanyClient {

    // 없거나 삭제된 업체면 COMPANY_NOT_FOUND(404)
    @GetMapping("/api/internal/companies/{companyId}")
    void validateCompanyExists(@PathVariable("companyId") UUID companyId);
}