package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.common.exception.GlobalExceptionHandler;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.application.InventoryService;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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