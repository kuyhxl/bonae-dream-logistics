package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.application.CompanyService;
import com.bonae.logistics.company.auth.RoleCheck;
import com.bonae.logistics.company.auth.UserRole;
import com.bonae.logistics.company.presentation.dto.request.ReqCreateCompanyDto;
import com.bonae.logistics.company.presentation.dto.request.ReqUpdateCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResCreateCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyListDto;
import com.bonae.logistics.company.presentation.dto.response.ResSearchCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResUpdateCompanyDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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

    @GetMapping
    @RoleCheck({UserRole.MASTER, UserRole.COMPANY_MANAGER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    public ResponseEntity<PageResponseDto<ResGetCompanyListDto>> getCompanies(
            @ModelAttribute PageRequestDto pageRequestDto,
            @RequestParam(defaultValue = "ALL") String type) {
        PageResponseDto<ResGetCompanyListDto> resDto = companyService.getCompanies(pageRequestDto, type);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    @GetMapping("/{companyId}")
    @RoleCheck({UserRole.MASTER, UserRole.COMPANY_MANAGER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    public ResponseEntity<ResGetCompanyDto> getCompany(@PathVariable UUID companyId) {
        ResGetCompanyDto resDto = companyService.getCompany(companyId);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    @GetMapping("/search")
    @RoleCheck({UserRole.MASTER, UserRole.COMPANY_MANAGER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    public ResponseEntity<PageResponseDto<ResSearchCompanyDto>> searchCompanies(
            @ModelAttribute PageRequestDto pageRequestDto,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(required = false) UUID hubId) {
        PageResponseDto<ResSearchCompanyDto> resDto = companyService.searchCompanies(pageRequestDto, keyword, type, hubId);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    @PatchMapping("/{companyId}")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.COMPANY_MANAGER})
    public ResponseEntity<ResUpdateCompanyDto> updateCompany(
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-User-Id") String username,
            @PathVariable UUID companyId,
            @Valid @RequestBody ReqUpdateCompanyDto reqDto) {
        ResUpdateCompanyDto resDto = companyService.updateCompany(
                companyId, reqDto, UserRole.valueOf(userRole), username);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }
}
