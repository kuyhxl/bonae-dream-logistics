package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.application.ProductService;
import com.bonae.logistics.company.auth.RoleCheck;
import com.bonae.logistics.company.auth.UserRole;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductListDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductDto;
import com.bonae.logistics.company.presentation.dto.request.ReqCreateProductDto;
import com.bonae.logistics.company.presentation.dto.response.ResCreateProductDto;
import com.bonae.logistics.company.presentation.dto.response.ResSearchProductDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/search")
    @RoleCheck({UserRole.MASTER, UserRole.COMPANY_MANAGER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    public ResponseEntity<PageResponseDto<ResSearchProductDto>> searchProducts(
            @ModelAttribute PageRequestDto pageRequestDto,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID companyId) {
        PageResponseDto<ResSearchProductDto> resDto = productService.searchProducts(pageRequestDto, keyword, companyId);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    @GetMapping("/{productId}")
    @RoleCheck({UserRole.MASTER, UserRole.COMPANY_MANAGER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    public ResponseEntity<ResGetProductDto> getProduct(@PathVariable UUID productId) {
        ResGetProductDto resDto = productService.getProduct(productId);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    @PostMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.COMPANY_MANAGER})
    public ResponseEntity<ResCreateProductDto> createProduct(
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-User-Id") String username,
            @Valid @RequestBody ReqCreateProductDto reqDto) {
        ResCreateProductDto resDto = productService.createProduct(reqDto, UserRole.valueOf(userRole), username);
        return ResponseEntity.status(HttpStatus.CREATED).body(resDto);
    }

    @GetMapping
    @RoleCheck({UserRole.MASTER, UserRole.COMPANY_MANAGER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    public ResponseEntity<PageResponseDto<ResGetProductListDto>> getProducts(@ModelAttribute PageRequestDto pageRequestDto) {
        PageResponseDto<ResGetProductListDto> resDto = productService.getProducts(pageRequestDto);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    @DeleteMapping("/{productId}")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    public ResponseEntity<Void> deleteProduct(
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-User-Id") String username,
            @PathVariable UUID productId) {
        productService.deleteProduct(productId, UserRole.valueOf(userRole), username);
        return ResponseEntity.noContent().build();
    }
}