package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.company.application.CompanyService;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyInternalDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/companies")
@RequiredArgsConstructor
public class InternalCompanyController {

    private final CompanyService companyService;

    @GetMapping("/{companyId}")
    public ResponseEntity<ResGetCompanyInternalDto> getCompany(@PathVariable UUID companyId) {
        ResGetCompanyInternalDto resDto = companyService.getCompanyInternal(companyId);
        return ResponseEntity.ok(resDto);
    }
}