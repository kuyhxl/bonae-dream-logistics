package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.exception.GlobalExceptionHandler;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.application.InventoryService;
import com.bonae.logistics.company.presentation.dto.request.ReqUpdateInventoryDto;
import com.bonae.logistics.company.presentation.dto.response.ResUpdateInventoryDto;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalInventoryController.class)
@Import(GlobalExceptionHandler.class)
class InternalInventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private Tracer tracer;

    @Test
    @DisplayName("PATCH /api/internal/inventories/{inventoryId}_정상요청시_변경결과를_응답한다")
    void updateInventory_정상요청시_변경결과를응답한다() throws Exception {
        UUID inventoryId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        ResUpdateInventoryDto resDto = ResUpdateInventoryDto.of(
                orderId, inventoryId, 100, 50, 50, "DECREASE", LocalDateTime.now());

        when(inventoryService.updateInventory(eq(inventoryId), any(ReqUpdateInventoryDto.class)))
                .thenReturn(resDto);

        String requestBody = """
                {
                  "orderId": "%s",
                  "quantity": 50,
                  "type": "DECREASE"
                }
                """.formatted(orderId);

        mockMvc.perform(patch("/api/internal/inventories/{inventoryId}", inventoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.inventoryId").value(inventoryId.toString()))
                .andExpect(jsonPath("$.beforeQuantity").value(100))
                .andExpect(jsonPath("$.changedQuantity").value(50))
                .andExpect(jsonPath("$.afterQuantity").value(50))
                .andExpect(jsonPath("$.type").value("DECREASE"));
    }

    @Test
    @DisplayName("PATCH /api/internal/inventories/{inventoryId}_orderId가없을때_400을응답한다")
    void updateInventory_orderId없음_400() throws Exception {
        String requestBody = """
                {
                  "quantity": 50,
                  "type": "DECREASE"
                }
                """;

        mockMvc.perform(patch("/api/internal/inventories/{inventoryId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fields[0].field").value("orderId"));
    }

    @Test
    @DisplayName("PATCH /api/internal/inventories/{inventoryId}_quantity가없을때_400을응답한다")
    void updateInventory_quantity없음_400() throws Exception {
        String requestBody = """
                {
                  "orderId": "%s",
                  "type": "DECREASE"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(patch("/api/internal/inventories/{inventoryId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fields[0].field").value("quantity"));
    }

    @Test
    @DisplayName("PATCH /api/internal/inventories/{inventoryId}_type이빈문자열일때_400을응답한다")
    void updateInventory_type빈문자열_400() throws Exception {
        String requestBody = """
                {
                  "orderId": "%s",
                  "quantity": 50,
                  "type": ""
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(patch("/api/internal/inventories/{inventoryId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fields[0].field").value("type"));
    }

    @Test
    @DisplayName("PATCH /api/internal/inventories/{inventoryId}_재고가없을때_404를응답한다")
    void updateInventory_재고없음_404() throws Exception {
        when(inventoryService.updateInventory(any(UUID.class), any(ReqUpdateInventoryDto.class)))
                .thenThrow(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));

        String requestBody = """
                {
                  "orderId": "%s",
                  "quantity": 50,
                  "type": "DECREASE"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(patch("/api/internal/inventories/{inventoryId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVENTORY_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /api/internal/inventories/{inventoryId}_재고가부족할때_409를응답한다")
    void updateInventory_재고부족_409() throws Exception {
        when(inventoryService.updateInventory(any(UUID.class), any(ReqUpdateInventoryDto.class)))
                .thenThrow(new BusinessException(ErrorCode.STOCK_SHORTAGE));

        String requestBody = """
                {
                  "orderId": "%s",
                  "quantity": 999999,
                  "type": "DECREASE"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(patch("/api/internal/inventories/{inventoryId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STOCK_SHORTAGE"));
    }

    @Test
    @DisplayName("PATCH /api/internal/inventories/{inventoryId}_quantity가0이하일때_400을응답한다")
    void updateInventory_quantity0이하_400() throws Exception {
        when(inventoryService.updateInventory(any(UUID.class), any(ReqUpdateInventoryDto.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_QUANTITY));

        String requestBody = """
                {
                  "orderId": "%s",
                  "quantity": 0,
                  "type": "DECREASE"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(patch("/api/internal/inventories/{inventoryId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_QUANTITY"));
    }

    @Test
    @DisplayName("PATCH /api/internal/inventories/{inventoryId}_type이DECREASE나RESTORE가아닐때_400을응답한다")
    void updateInventory_유효하지않은type_400() throws Exception {
        when(inventoryService.updateInventory(any(UUID.class), any(ReqUpdateInventoryDto.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_INVENTORY_TYPE));

        String requestBody = """
                {
                  "orderId": "%s",
                  "quantity": 50,
                  "type": "INVALID"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(patch("/api/internal/inventories/{inventoryId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INVENTORY_TYPE"));
    }

    @Test
    @DisplayName("GET /api/internal/inventories/search_productId_hubId_모두_생략하면_null로_전체조회한다")
    void searchInventories_파라미터생략시_null로_조회() throws Exception {
        PageResponseDto<?> emptyPage = PageResponseDto.builder()
                .content(java.util.List.of())
                .page(1).size(10).totalElements(0).totalPages(0).last(true)
                .build();
        when(inventoryService.searchInventories(any(PageRequestDto.class), isNull(), isNull()))
                .thenReturn((PageResponseDto) emptyPage);

        mockMvc.perform(get("/api/internal/inventories/search"))
                .andExpect(status().isOk());

        verify(inventoryService).searchInventories(any(PageRequestDto.class), isNull(), isNull());
    }

    @Test
    @DisplayName("GET /api/internal/inventories/search_productId가_빈문자열이면_null로_전체조회한다")
    void searchInventories_productId빈문자열_null로_조회() throws Exception {
        PageResponseDto<?> emptyPage = PageResponseDto.builder()
                .content(java.util.List.of())
                .page(1).size(10).totalElements(0).totalPages(0).last(true)
                .build();
        when(inventoryService.searchInventories(any(PageRequestDto.class), isNull(), isNull()))
                .thenReturn((PageResponseDto) emptyPage);

        mockMvc.perform(get("/api/internal/inventories/search").param("productId", ""))
                .andExpect(status().isOk());

        verify(inventoryService).searchInventories(any(PageRequestDto.class), isNull(), isNull());
    }

    @Test
    @DisplayName("GET /api/internal/inventories/search_productId가_UUID형식이_아니면_400을_응답한다")
    void searchInventories_productId형식오류_400() throws Exception {
        mockMvc.perform(get("/api/internal/inventories/search").param("productId", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}