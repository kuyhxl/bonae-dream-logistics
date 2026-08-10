package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.exception.GlobalExceptionHandler;
import com.bonae.logistics.company.application.CompanyService;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyInternalDto;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalCompanyController.class)
@Import(GlobalExceptionHandler.class)
class InternalCompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    // GlobalExceptionHandler(common 모듈)가 요구하는 의존성. 이 슬라이스 테스트에도 함께 로드되므로 목으로 채워준다.
    @MockitoBean
    private Tracer tracer;

    @Test
    @DisplayName("GET /api/internal/companies/{companyId}_권한헤더없이도_삭제되지않은업체정보를_응답한다")
    void getCompany_권한헤더없이도_삭제되지않은업체정보를_응답한다() throws Exception {
        UUID companyId = UUID.randomUUID();
        ResGetCompanyInternalDto resDto = ResGetCompanyInternalDto.builder()
                .companyId(companyId)
                .name("삼성전자")
                .type(CompanyType.PRODUCER)
                .hubId(UUID.randomUUID())
                .address("경기도 수원시 영통구")
                .isDeleted(false)
                .build();

        when(companyService.getCompanyInternal(companyId)).thenReturn(resDto);

        // 내부 API는 X-User-Role 헤더 없이도 통과해야 한다 (인가 대상에서 제외됨)
        mockMvc.perform(get("/api/internal/companies/{companyId}", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.name").value("삼성전자"))
                .andExpect(jsonPath("$.type").value("PRODUCER"))
                .andExpect(jsonPath("$.hubId").value(resDto.getHubId().toString()))
                .andExpect(jsonPath("$.address").value("경기도 수원시 영통구"))
                .andExpect(jsonPath("$.isDeleted").value(false));
    }

    @Test
    @DisplayName("GET /api/internal/companies/{companyId}_존재하지않거나삭제된업체일때_404를응답한다")
    void getCompany_존재하지않거나삭제된업체일때_404를응답한다() throws Exception {
        UUID companyId = UUID.randomUUID();
        when(companyService.getCompanyInternal(companyId)).thenThrow(new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        mockMvc.perform(get("/api/internal/companies/{companyId}", companyId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }
}