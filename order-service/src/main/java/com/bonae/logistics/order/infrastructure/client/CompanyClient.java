package com.bonae.logistics.order.infrastructure.client;

import com.bonae.logistics.order.infrastructure.client.dto.response.CompanyInfoResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "company-service")
public interface CompanyClient {

    @GetMapping("/api/internal/companies/{companyId}")
    CompanyInfoResponseDto getCompany(@PathVariable UUID companyId);
}