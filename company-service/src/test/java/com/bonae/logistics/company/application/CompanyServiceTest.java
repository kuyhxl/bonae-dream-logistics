package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.company.domain.Company;
import com.bonae.logistics.company.domain.CompanyType;
import com.bonae.logistics.company.infrastructure.CompanyRepository;
import com.bonae.logistics.company.presentation.ReqCreateCompanyDto;
import com.bonae.logistics.company.presentation.ResCreateCompanyDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyService companyService;

    @Test
    @DisplayName("createCompany_중복된업체가없을때_업체생성성공")
    void createCompany_중복된업체가없을때_업체생성성공() {
        ReqCreateCompanyDto reqDto = ReqCreateCompanyDto.builder()
                .name("배송센터A")
                .type(CompanyType.PRODUCER)
                .hubId(UUID.randomUUID())
                .address("서울시 강남구 테헤란로 1")
                .build();

        Company savedCompany =
                new Company(reqDto.getName(), reqDto.getType(), reqDto.getHubId(), reqDto.getAddress());

        when(companyRepository.existsByNameAndAddressAndDeletedAtIsNull(reqDto.getName(), reqDto.getAddress()))
                .thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);

        ResCreateCompanyDto resDto = companyService.createCompany(reqDto);

        assertThat(resDto.getName()).isEqualTo(reqDto.getName());
        assertThat(resDto.getType()).isEqualTo(reqDto.getType());
        assertThat(resDto.getHubId()).isEqualTo(reqDto.getHubId());
        assertThat(resDto.getAddress()).isEqualTo(reqDto.getAddress());
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    @DisplayName("createCompany_동일한이름과주소의업체가이미존재할때_예외발생")
    void createCompany_동일한이름과주소업체가이미존재할때_예외발생() {
        ReqCreateCompanyDto reqDto = ReqCreateCompanyDto.builder()
                .name("배송센터A")
                .type(CompanyType.PRODUCER)
                .hubId(UUID.randomUUID())
                .address("서울시 강남구 테헤란로 1")
                .build();

        when(companyRepository.existsByNameAndAddressAndDeletedAtIsNull(reqDto.getName(), reqDto.getAddress()))
                .thenReturn(true);

        assertThatThrownBy(() -> companyService.createCompany(reqDto))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_DUPLICATED);

        verify(companyRepository, never()).save(any(Company.class));
    }
}