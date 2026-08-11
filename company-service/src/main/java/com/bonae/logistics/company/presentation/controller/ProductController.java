package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.application.ProductService;
import com.bonae.logistics.company.auth.RoleCheck;
import com.bonae.logistics.company.auth.UserRole;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductListDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @RoleCheck({UserRole.MASTER, UserRole.COMPANY_MANAGER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    public ResponseEntity<PageResponseDto<ResGetProductListDto>> getProducts(@ModelAttribute PageRequestDto pageRequestDto) {
        PageResponseDto<ResGetProductListDto> resDto = productService.getProducts(pageRequestDto);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }
}