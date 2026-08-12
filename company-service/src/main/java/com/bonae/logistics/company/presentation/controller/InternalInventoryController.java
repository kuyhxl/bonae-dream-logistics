package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.company.application.InventoryService;
import com.bonae.logistics.company.presentation.dto.request.ReqUpdateInventoryDto;
import com.bonae.logistics.company.presentation.dto.response.ResUpdateInventoryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/inventories")
@RequiredArgsConstructor
public class InternalInventoryController {

    private final InventoryService inventoryService;

    @PatchMapping("/{inventoryId}")
    public ResponseEntity<ResUpdateInventoryDto> updateInventory(
            @PathVariable UUID inventoryId,
            @Valid @RequestBody ReqUpdateInventoryDto reqDto) {
        ResUpdateInventoryDto resDto = inventoryService.updateInventory(inventoryId, reqDto);
        return ResponseEntity.ok(resDto);
    }
}