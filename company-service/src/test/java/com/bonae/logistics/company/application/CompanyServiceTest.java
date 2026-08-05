package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.company.domain.Company;
import com.bonae.logistics.company.domain.CompanyType;
import com.bonae.logistics.company.infrastructure.CompanyRepository;
import com.bonae.logistics.company.presentation.ReqCreateCompanyDto;
import com.bonae.logistics.company.presentation.ResCreateCompanyDto;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
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
    @DisplayName("createCompany_중복된 업체가 없을 때_업체생성성공")
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
        when(companyRepository.saveAndFlush(any(Company.class))).thenReturn(savedCompany);

        ResCreateCompanyDto resDto = companyService.createCompany(reqDto);

        assertThat(resDto.getName()).isEqualTo(reqDto.getName());
        assertThat(resDto.getType()).isEqualTo(reqDto.getType());
        assertThat(resDto.getHubId()).isEqualTo(reqDto.getHubId());
        assertThat(resDto.getAddress()).isEqualTo(reqDto.getAddress());
        verify(companyRepository).saveAndFlush(any(Company.class));
    }

    @Test
    @DisplayName("createCompany_동일한 이름과 주소의 업체가 이미 존재할 때_예외발생")
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

        verify(companyRepository, never()).saveAndFlush(any(Company.class));
    }

    @Test
    @DisplayName("createCompany_사전검증 통과 후 저장시점에 이름+주소 유니크 제약조건 위반이 발생할때_예외발생")
    void createCompany_사전검증통과후저장시점에유니크제약조건위반이발생할때_예외발생() {
        ReqCreateCompanyDto reqDto = ReqCreateCompanyDto.builder()
                .name("배송센터A")
                .type(CompanyType.PRODUCER)
                .hubId(UUID.randomUUID())
                .address("서울시 강남구 테헤란로 1")
                .build();

        // 동시 요청 등으로 existsBy 체크는 통과했지만, 저장 시점에 DB 부분 유니크 인덱스에 걸리는 경우
        when(companyRepository.existsByNameAndAddressAndDeletedAtIsNull(reqDto.getName(), reqDto.getAddress()))
                .thenReturn(false);
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenThrow(duplicateKeyException("ux_p_companies_name_address_active"));

        assertThatThrownBy(() -> companyService.createCompany(reqDto))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_DUPLICATED);

        verify(companyRepository).saveAndFlush(any(Company.class));
    }

    @Test
    @DisplayName("createCompany_저장시점에 이름+주소 유니크 제약조건이 아닌 다른 제약조건 위반이 발생할때_원본예외그대로전파")
    void createCompany_다른제약조건위반이발생할때_원본예외그대로전파() {
        ReqCreateCompanyDto reqDto = ReqCreateCompanyDto.builder()
                .name("배송센터A")
                .type(CompanyType.PRODUCER)
                .hubId(UUID.randomUUID())
                .address("서울시 강남구 테헤란로 1")
                .build();

        when(companyRepository.existsByNameAndAddressAndDeletedAtIsNull(reqDto.getName(), reqDto.getAddress()))
                .thenReturn(false);
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenThrow(duplicateKeyException("pk_p_companies"));

        assertThatThrownBy(() -> companyService.createCompany(reqDto))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(BusinessException.class);

        verify(companyRepository).saveAndFlush(any(Company.class));
    }

    private DataIntegrityViolationException duplicateKeyException(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException(
                "could not execute statement",
                new SQLException("duplicate key value violates unique constraint \"" + constraintName + "\""),
                constraintName
        );
        return new DataIntegrityViolationException("duplicate key", cause);
    }
}