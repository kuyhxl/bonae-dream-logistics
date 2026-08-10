package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.exception.GlobalExceptionHandler;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.application.CompanyService;
import com.bonae.logistics.company.auth.UserRole;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.bonae.logistics.company.presentation.dto.request.ReqUpdateCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyListDto;
import com.bonae.logistics.company.presentation.dto.response.ResSearchCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResUpdateCompanyDto;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanyController.class)
@Import(GlobalExceptionHandler.class)
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    // GlobalExceptionHandler(common 모듈)가 요구하는 의존성. 이 슬라이스 테스트에도 함께 로드되므로 목으로 채워준다.
    @MockitoBean
    private Tracer tracer;

    @Test
    @DisplayName("GET /api/companies_정상요청시_업체목록과페이지정보를_응답한다")
    void getCompanies_정상요청시_업체목록과페이지정보를_응답한다() throws Exception {
        ResGetCompanyListDto item = ResGetCompanyListDto.builder()
                .companyId(UUID.randomUUID())
                .name("배송센터A")
                .type(CompanyType.PRODUCER)
                .hubId(UUID.randomUUID())
                .address("서울시 강남구 테헤란로 1")
                .createdAt(LocalDateTime.now())
                .createdBy("admin")
                .build();

        PageResponseDto<ResGetCompanyListDto> pageResponse = PageResponseDto.<ResGetCompanyListDto>builder()
                .content(List.of(item))
                .page(1)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(companyService.getCompanies(any(PageRequestDto.class), anyString())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/companies")
                        .header("X-User-Role", "MASTER")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "createdAt")
                        .param("direction", "DESC")
                        .param("type", "PRODUCER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("배송센터A"))
                .andExpect(jsonPath("$.content[0].type").value("PRODUCER"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("GET /api/companies_권한헤더가없을때_401을응답한다")
    void getCompanies_권한헤더가없을때_401을응답한다() throws Exception {
        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /api/companies/search_정상요청시_검색결과와페이지정보를_응답한다")
    void searchCompanies_정상요청시_검색결과와페이지정보를_응답한다() throws Exception {
        UUID hubId = UUID.randomUUID();
        ResSearchCompanyDto item = ResSearchCompanyDto.builder()
                .companyId(UUID.randomUUID())
                .name("삼성전자")
                .type(CompanyType.PRODUCER)
                .hubId(hubId)
                .address("경기도 수원시 영통구")
                .build();

        PageResponseDto<ResSearchCompanyDto> pageResponse = PageResponseDto.<ResSearchCompanyDto>builder()
                .content(List.of(item))
                .page(1)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(companyService.searchCompanies(any(PageRequestDto.class), eq("삼성"), eq("PRODUCER"), eq(hubId)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/companies/search")
                        .header("X-User-Role", "DELIVERY_MANAGER")
                        .param("keyword", "삼성")
                        .param("type", "PRODUCER")
                        .param("hubId", hubId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("삼성전자"))
                .andExpect(jsonPath("$.content[0].type").value("PRODUCER"))
                .andExpect(jsonPath("$.content[0].hubId").value(hubId.toString()));
    }

    @Test
    @DisplayName("GET /api/companies/search_권한헤더가없을때_401을응답한다")
    void searchCompanies_권한헤더가없을때_401을응답한다() throws Exception {
        mockMvc.perform(get("/api/companies/search"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /api/companies/search_허용되지않은정렬기준일때_400을응답한다")
    void searchCompanies_허용되지않은정렬기준일때_400을응답한다() throws Exception {
        when(companyService.searchCompanies(any(PageRequestDto.class), any(), anyString(), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_SORT_FIELD));

        mockMvc.perform(get("/api/companies/search")
                        .header("X-User-Role", "MASTER")
                        .param("sort", "invalidField"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));
    }

    @Test
    @DisplayName("GET /api/companies/{companyId}_정상요청시_업체정보를_응답한다")
    void getCompany_정상요청시_업체정보를_응답한다() throws Exception {
        UUID companyId = UUID.randomUUID();
        ResGetCompanyDto resDto = ResGetCompanyDto.builder()
                .companyId(companyId)
                .name("삼성전자")
                .type(CompanyType.PRODUCER)
                .hubId(UUID.randomUUID())
                .address("경기도 수원시 영통구")
                .createdAt(LocalDateTime.now())
                .createdBy("admin-id")
                .updatedAt(LocalDateTime.now())
                .updatedBy("admin02")
                .build();

        when(companyService.getCompany(companyId)).thenReturn(resDto);

        mockMvc.perform(get("/api/companies/{companyId}", companyId)
                        .header("X-User-Role", "MASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.name").value("삼성전자"))
                .andExpect(jsonPath("$.type").value("PRODUCER"))
                .andExpect(jsonPath("$.hubId").value(resDto.getHubId().toString()))
                .andExpect(jsonPath("$.address").value("경기도 수원시 영통구"))
                .andExpect(jsonPath("$.createdBy").value("admin-id"))
                .andExpect(jsonPath("$.updatedBy").value("admin02"));
    }

    @Test
    @DisplayName("GET /api/companies/{companyId}_권한헤더가없을때_403을응답한다")
    void getCompany_권한헤더가없을때_401을응답한다() throws Exception {
        mockMvc.perform(get("/api/companies/{companyId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /api/companies/{companyId}_존재하지않거나삭제된업체일때_404를응답한다")
    void getCompany_존재하지않거나삭제된업체일때_404를응답한다() throws Exception {
        UUID companyId = UUID.randomUUID();
        when(companyService.getCompany(companyId)).thenThrow(new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        mockMvc.perform(get("/api/companies/{companyId}", companyId)
                        .header("X-User-Role", "MASTER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /api/companies/{companyId}_정상요청시_수정된업체정보를_응답한다")
    void updateCompany_정상요청시_수정된업체정보를_응답한다() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        ResUpdateCompanyDto resDto = ResUpdateCompanyDto.builder()
                .companyId(companyId)
                .name("삼성전자 물류센터")
                .type(CompanyType.PRODUCER)
                .hubId(hubId)
                .address("경기도 수원시 권선구")
                .updatedAt(LocalDateTime.now())
                .updatedBy("hub-admin-id")
                .build();

        when(companyService.updateCompany(eq(companyId), any(ReqUpdateCompanyDto.class), eq(UserRole.MASTER), anyString()))
                .thenReturn(resDto);

        String requestBody = """
                {
                  "name": "삼성전자 물류센터",
                  "type": "PRODUCER",
                  "hubId": "%s",
                  "address": "경기도 수원시 권선구"
                }
                """.formatted(hubId);

        mockMvc.perform(patch("/api/companies/{companyId}", companyId)
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Id", "master01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.name").value("삼성전자 물류센터"))
                .andExpect(jsonPath("$.type").value("PRODUCER"))
                .andExpect(jsonPath("$.hubId").value(hubId.toString()))
                .andExpect(jsonPath("$.address").value("경기도 수원시 권선구"))
                .andExpect(jsonPath("$.updatedBy").value("hub-admin-id"));
    }

    @Test
    @DisplayName("PATCH /api/companies/{companyId}_권한헤더가없을때_401을응답한다")
    void updateCompany_권한헤더가없을때_401을응답한다() throws Exception {
        mockMvc.perform(patch("/api/companies/{companyId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("PATCH /api/companies/{companyId}_존재하지않거나삭제된업체일때_404를응답한다")
    void updateCompany_존재하지않거나삭제된업체일때_404를응답한다() throws Exception {
        UUID companyId = UUID.randomUUID();
        when(companyService.updateCompany(eq(companyId), any(ReqUpdateCompanyDto.class), eq(UserRole.MASTER), anyString()))
                .thenThrow(new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        mockMvc.perform(patch("/api/companies/{companyId}", companyId)
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Id", "master01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/companies/{companyId}_정상요청시_204를응답한다")
    void deleteCompany_정상요청시_204를응답한다() throws Exception {
        UUID companyId = UUID.randomUUID();

        mockMvc.perform(delete("/api/companies/{companyId}", companyId)
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Id", "master01"))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    @DisplayName("DELETE /api/companies/{companyId}_권한헤더가없을때_401을응답한다")
    void deleteCompany_권한헤더가없을때_401을응답한다() throws Exception {
        mockMvc.perform(delete("/api/companies/{companyId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("DELETE /api/companies/{companyId}_허용되지않은역할일때_403을응답한다")
    void deleteCompany_허용되지않은역할일때_403을응답한다() throws Exception {
        mockMvc.perform(delete("/api/companies/{companyId}", UUID.randomUUID())
                        .header("X-User-Role", "COMPANY_MANAGER")
                        .header("X-User-Id", "company-manager01"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("DELETE /api/companies/{companyId}_존재하지않거나삭제된업체일때_404를응답한다")
    void deleteCompany_존재하지않거나삭제된업체일때_404를응답한다() throws Exception {
        UUID companyId = UUID.randomUUID();
        doThrow(new BusinessException(ErrorCode.COMPANY_NOT_FOUND))
                .when(companyService).deleteCompany(eq(companyId), eq(UserRole.MASTER), anyString());

        mockMvc.perform(delete("/api/companies/{companyId}", companyId)
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Id", "master01"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
    }
}