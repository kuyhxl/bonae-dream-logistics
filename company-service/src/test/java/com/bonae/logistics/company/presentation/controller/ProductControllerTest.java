package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.exception.GlobalExceptionHandler;
import com.bonae.logistics.company.application.ProductService;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductDto;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    // GlobalExceptionHandler(common 모듈)가 요구하는 의존성. 이 슬라이스 테스트에도 함께 로드되므로 목으로 채워준다.
    @MockitoBean
    private Tracer tracer;

    @Test
    @DisplayName("GET /api/products/{productId}_정상요청시_상품정보를_응답한다")
    void getProduct_정상요청시_상품정보를_응답한다() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        ResGetProductDto resDto = ResGetProductDto.builder()
                .productId(productId)
                .name("갤럭시 스마트폰")
                .companyId(companyId)
                .price(new BigDecimal("1200000.00"))
                .createdAt(LocalDateTime.now())
                .createdBy("admin-id")
                .updatedAt(LocalDateTime.now())
                .updatedBy("admin-id")
                .build();

        when(productService.getProduct(productId)).thenReturn(resDto);

        mockMvc.perform(get("/api/products/{productId}", productId)
                        .header("X-User-Role", "MASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.name").value("갤럭시 스마트폰"))
                .andExpect(jsonPath("$.companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.price").value(1200000.00))
                .andExpect(jsonPath("$.createdBy").value("admin-id"))
                .andExpect(jsonPath("$.updatedBy").value("admin-id"));
    }

    @Test
    @DisplayName("GET /api/products/{productId}_권한헤더가없을때_401을응답한다")
    void getProduct_권한헤더가없을때_401을응답한다() throws Exception {
        mockMvc.perform(get("/api/products/{productId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /api/products/{productId}_존재하지않거나삭제된상품일때_404를응답한다")
    void getProduct_존재하지않거나삭제된상품일때_404를응답한다() throws Exception {
        UUID productId = UUID.randomUUID();
        when(productService.getProduct(productId)).thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(get("/api/products/{productId}", productId)
                        .header("X-User-Role", "MASTER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }
}