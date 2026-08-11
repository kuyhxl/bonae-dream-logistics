package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.company.application.ProductService;
import com.bonae.logistics.company.auth.RoleCheck;
import com.bonae.logistics.company.auth.UserRole;
import com.bonae.logistics.company.presentation.dto.request.ReqCreateProductDto;
import com.bonae.logistics.company.presentation.dto.response.ResCreateProductDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.COMPANY_MANAGER})
    public ResponseEntity<ResCreateProductDto> createProduct(
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-User-Id") String username,
            @Valid @RequestBody ReqCreateProductDto reqDto) {
        ResCreateProductDto resDto = productService.createProduct(reqDto, UserRole.valueOf(userRole), username);
        return ResponseEntity.status(HttpStatus.CREATED).body(resDto);
    }
}