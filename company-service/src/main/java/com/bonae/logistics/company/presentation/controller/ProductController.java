package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.company.application.ProductService;
import com.bonae.logistics.company.auth.RoleCheck;
import com.bonae.logistics.company.auth.UserRole;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    @RoleCheck({UserRole.MASTER, UserRole.COMPANY_MANAGER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    public ResponseEntity<ResGetProductDto> getProduct(@PathVariable UUID productId) {
        ResGetProductDto resDto = productService.getProduct(productId);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }
}