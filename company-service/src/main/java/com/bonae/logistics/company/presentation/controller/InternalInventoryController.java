package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.application.InventoryService;
import com.bonae.logistics.company.presentation.dto.response.ResSearchInventoryInternalDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/inventories")
@RequiredArgsConstructor
public class InternalInventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/search")
    public ResponseEntity<PageResponseDto<ResSearchInventoryInternalDto>> searchInventories(
            @ModelAttribute PageRequestDto pageRequestDto,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID hubId) {
        PageResponseDto<ResSearchInventoryInternalDto> resDto = inventoryService.searchInventories(pageRequestDto, productId, hubId);
        return ResponseEntity.ok(resDto);
    }
}
