package com.bonae.logistics.company.presentation;

import com.bonae.logistics.company.application.CompanyService;
import com.bonae.logistics.company.auth.RoleCheck;
import com.bonae.logistics.company.auth.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    public ResponseEntity<ResCreateCompanyDto> createCompany(@Valid @RequestBody ReqCreateCompanyDto reqDto) {
        ResCreateCompanyDto resDto = companyService.createCompany(reqDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resDto);
    }
}
