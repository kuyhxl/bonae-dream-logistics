package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.company.application.ProductService;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductInternalDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    public ResponseEntity<ResGetProductInternalDto> getProduct(@PathVariable UUID productId) {
        ResGetProductInternalDto resDto = productService.getProductInternal(productId);
        return ResponseEntity.ok(resDto);
    }
}