package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.exception.GlobalExceptionHandler;
import com.bonae.logistics.company.application.ProductService;
import com.bonae.logistics.company.auth.UserRole;
import com.bonae.logistics.company.presentation.dto.request.ReqCreateProductDto;
import com.bonae.logistics.company.presentation.dto.response.ResCreateProductDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductDto;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("POST /api/products_정상요청시_생성된상품정보를_응답한다")
    void createProduct_정상요청시_생성된상품정보를_응답한다() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        ResCreateProductDto resDto = ResCreateProductDto.builder()
                .productId(UUID.randomUUID())
                .name("갤럭시 스마트폰")
                .companyId(companyId)
                .price(new BigDecimal("1200000.00"))
                .hubId(hubId)
                .quantity(100)
                .createdAt(LocalDateTime.now())
                .createdBy("admin-id")
                .build();

        when(productService.createProduct(any(ReqCreateProductDto.class), eq(UserRole.MASTER), anyString()))
                .thenReturn(resDto);

        String requestBody = """
                {
                  "name": "갤럭시 스마트폰",
                  "companyId": "%s",
                  "price": 1200000.00,
                  "hubId": "%s",
                  "quantity": 100
                }
                """.formatted(companyId, hubId);

        mockMvc.perform(post("/api/products")
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Id", "admin-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("갤럭시 스마트폰"))
                .andExpect(jsonPath("$.companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.price").value(1200000.00))
                .andExpect(jsonPath("$.hubId").value(hubId.toString()))
                .andExpect(jsonPath("$.quantity").value(100))
                .andExpect(jsonPath("$.createdBy").value("admin-id"));
    }

    @Test
    @DisplayName("POST /api/products_권한헤더가없을때_401을응답한다")
    void createProduct_권한헤더가없을때_401을응답한다() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("POST /api/products_허용되지않은역할일때_403을응답한다")
    void createProduct_허용되지않은역할일때_403을응답한다() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("X-User-Role", "DELIVERY_MANAGER")
                        .header("X-User-Id", "delivery01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /api/products_필수값이없을때_400을응답한다")
    void createProduct_필수값이없을때_400을응답한다() throws Exception {
        String requestBody = """
                {
                  "companyId": "%s",
                  "price": 1000.00,
                  "hubId": "%s",
                  "quantity": 10
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/products")
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Id", "admin-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fields[0].field").value("name"));
    }

    @Test
    @DisplayName("POST /api/products_업체가존재하지않을때_404를응답한다")
    void createProduct_업체가존재하지않을때_404를응답한다() throws Exception {
        when(productService.createProduct(any(ReqCreateProductDto.class), eq(UserRole.MASTER), anyString()))
                .thenThrow(new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        String requestBody = """
                {
                  "name": "갤럭시 스마트폰",
                  "companyId": "%s",
                  "price": 1000.00,
                  "hubId": "%s",
                  "quantity": 10
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/products")
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Id", "admin-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/products_허브가존재하지않을때_404를응답한다")
    void createProduct_허브가존재하지않을때_404를응답한다() throws Exception {
        when(productService.createProduct(any(ReqCreateProductDto.class), eq(UserRole.MASTER), anyString()))
                .thenThrow(new BusinessException(ErrorCode.HUB_NOT_FOUND));

        String requestBody = """
                {
                  "name": "갤럭시 스마트폰",
                  "companyId": "%s",
                  "price": 1000.00,
                  "hubId": "%s",
                  "quantity": 10
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/products")
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Id", "admin-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HUB_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/products_가격이유효하지않을때_400을응답한다")
    void createProduct_가격이유효하지않을때_400을응답한다() throws Exception {
        when(productService.createProduct(any(ReqCreateProductDto.class), eq(UserRole.MASTER), anyString()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_PRICE));

        String requestBody = """
                {
                  "name": "갤럭시 스마트폰",
                  "companyId": "%s",
                  "price": -1,
                  "hubId": "%s",
                  "quantity": 10
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/products")
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Id", "admin-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PRICE"));
    }

    @Test
    @DisplayName("POST /api/products_수량이유효하지않을때_400을응답한다")
    void createProduct_수량이유효하지않을때_400을응답한다() throws Exception {
        when(productService.createProduct(any(ReqCreateProductDto.class), eq(UserRole.MASTER), anyString()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_QUANTITY));

        String requestBody = """
                {
                  "name": "갤럭시 스마트폰",
                  "companyId": "%s",
                  "price": 1000.00,
                  "hubId": "%s",
                  "quantity": -5
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/products")
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Id", "admin-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_QUANTITY"));
    }

    @Test
    @DisplayName("POST /api/products_상품이이미존재할때_409를응답한다")
    void createProduct_상품이이미존재할때_409를응답한다() throws Exception {
        when(productService.createProduct(any(ReqCreateProductDto.class), eq(UserRole.MASTER), anyString()))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_DUPLICATED));

        String requestBody = """
                {
                  "name": "갤럭시 스마트폰",
                  "companyId": "%s",
                  "price": 1000.00,
                  "hubId": "%s",
                  "quantity": 10
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/products")
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Id", "admin-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_DUPLICATED"));
    }
}