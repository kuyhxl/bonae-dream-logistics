package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.exception.GlobalExceptionHandler;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.application.ProductService;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductListDto;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("GET /api/products_정상요청시_상품목록과페이지정보를_응답한다")
    void getProducts_정상요청시_상품목록과페이지정보를_응답한다() throws Exception {
        ResGetProductListDto item = ResGetProductListDto.builder()
                .productId(UUID.randomUUID())
                .name("갤럭시 스마트폰")
                .companyId(UUID.randomUUID())
                .price(new BigDecimal("1200000.00"))
                .build();

        PageResponseDto<ResGetProductListDto> pageResponse = PageResponseDto.<ResGetProductListDto>builder()
                .content(List.of(item))
                .page(1)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(productService.getProducts(any(PageRequestDto.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/products")
                        .header("X-User-Role", "MASTER")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "createdAt")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("갤럭시 스마트폰"))
                .andExpect(jsonPath("$.content[0].price").value(1200000.00))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("GET /api/products_권한헤더가없을때_401을응답한다")
    void getProducts_권한헤더가없을때_401을응답한다() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /api/products_허용되지않은정렬기준일때_400을응답한다")
    void getProducts_허용되지않은정렬기준일때_400을응답한다() throws Exception {
        when(productService.getProducts(any(PageRequestDto.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_SORT_FIELD));

        mockMvc.perform(get("/api/products")
                        .header("X-User-Role", "MASTER")
                        .param("sort", "invalidField"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));
    }
}