package com.bonae.logistics.delivery.infrastructure.client;

import com.bonae.logistics.delivery.infrastructure.client.dto.CompanyInfoClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "company-service")
public interface CompanyClient {

    @GetMapping("/api/internal/companies/{companyId}")
    CompanyInfoClientResponse getCompany(@PathVariable("companyId") UUID companyId);
}
