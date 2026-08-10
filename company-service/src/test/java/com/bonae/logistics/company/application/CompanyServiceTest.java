package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.auth.UserRole;
import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.bonae.logistics.company.domain.repository.CompanyRepository;
import com.bonae.logistics.company.infrastructure.HubClient;
import com.bonae.logistics.company.infrastructure.UserClient;
import com.bonae.logistics.company.infrastructure.UserInfoDto;
import com.bonae.logistics.company.presentation.dto.request.ReqCreateCompanyDto;
import com.bonae.logistics.company.presentation.dto.request.ReqUpdateCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResCreateCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyInternalDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyListDto;
import com.bonae.logistics.company.presentation.dto.response.ResUpdateCompanyDto;
import feign.FeignException;
import feign.Request;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private HubClient hubClient;

    @Mock
    private UserClient userClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    private static final String USERNAME = "manager01";

    @InjectMocks
    private CompanyService companyService;

    // TransactionTemplate은 실제 트랜잭션 없이 콜백을 그대로 실행해 단위 테스트에서 위임되도록 스텁한다.
    @BeforeEach
    void setUpTransactionTemplate() {
        lenient().when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
    }

    @Test
    @DisplayName("createCompany_존재하는 허브이고 중복된 업체가 없을 때_업체생성성공")
    void createCompany_중복된업체가없을때_업체생성성공() {
        ReqCreateCompanyDto reqDto = ReqCreateCompanyDto.builder()
                .name("배송센터A")
                .type(CompanyType.PRODUCER)
                .hubId(UUID.randomUUID())
                .address("서울시 강남구 테헤란로 1")
                .build();

        Company savedCompany =
                new Company(reqDto.getName(), reqDto.getType(), reqDto.getHubId(), reqDto.getAddress());

        when(hubClient.getHub(reqDto.getHubId())).thenReturn(ResponseEntity.ok().build());
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
    @DisplayName("createCompany_존재하지 않는 허브일 때_예외발생")
    void createCompany_존재하지않는허브일때_예외발생() {
        ReqCreateCompanyDto reqDto = ReqCreateCompanyDto.builder()
                .name("배송센터A")
                .type(CompanyType.PRODUCER)
                .hubId(UUID.randomUUID())
                .address("서울시 강남구 테헤란로 1")
                .build();

        when(hubClient.getHub(reqDto.getHubId())).thenThrow(hubNotFoundException(reqDto.getHubId()));

        assertThatThrownBy(() -> companyService.createCompany(reqDto))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.HUB_NOT_FOUND);

        verify(companyRepository, never()).existsByNameAndAddressAndDeletedAtIsNull(any(), any());
        verify(companyRepository, never()).saveAndFlush(any(Company.class));
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

    @Test
    @DisplayName("getCompanies_type이 ALL일때_전체업체를조회한다")
    void getCompanies_type이ALL일때_전체업체를조회한다() {
        Company company = new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        PageRequestDto pageRequestDto = new PageRequestDto();
        Page<Company> companyPage = new PageImpl<>(List.of(company), pageRequestDto.toPageable(), 1);

        when(companyRepository.findAllByTypeAndDeletedAtIsNull(isNull(), any(Pageable.class)))
                .thenReturn(companyPage);

        PageResponseDto<ResGetCompanyListDto> resDto = companyService.getCompanies(pageRequestDto, "ALL");

        assertThat(resDto.getContent()).hasSize(1);
        assertThat(resDto.getContent().get(0).getName()).isEqualTo(company.getName());
        assertThat(resDto.getTotalElements()).isEqualTo(1);
        verify(companyRepository).findAllByTypeAndDeletedAtIsNull(isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("getCompanies_특정업체타입으로조회시_해당타입만조회한다")
    void getCompanies_특정업체타입으로조회시_해당타입만조회한다() {
        PageRequestDto pageRequestDto = new PageRequestDto();
        Page<Company> emptyPage = new PageImpl<>(List.of(), pageRequestDto.toPageable(), 0);

        when(companyRepository.findAllByTypeAndDeletedAtIsNull(eq(CompanyType.PRODUCER), any(Pageable.class)))
                .thenReturn(emptyPage);

        companyService.getCompanies(pageRequestDto, "PRODUCER");

        verify(companyRepository).findAllByTypeAndDeletedAtIsNull(eq(CompanyType.PRODUCER), any(Pageable.class));
    }

    @Test
    @DisplayName("getCompanies_유효하지않은업체타입일때_예외발생")
    void getCompanies_유효하지않은업체타입일때_예외발생() {
        PageRequestDto pageRequestDto = new PageRequestDto();

        assertThatThrownBy(() -> companyService.getCompanies(pageRequestDto, "INVALID"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COMPANY_TYPE);

        verify(companyRepository, never()).findAllByTypeAndDeletedAtIsNull(any(), any());
    }

    @Test
    @DisplayName("getCompany_삭제되지않은업체일때_업체정보를반환한다")
    void getCompany_삭제되지않은업체일때_업체정보를반환한다() {
        Company company = new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));

        ResGetCompanyDto resDto = companyService.getCompany(company.getId());

        assertThat(resDto.getCompanyId()).isEqualTo(company.getId());
        assertThat(resDto.getName()).isEqualTo(company.getName());
        assertThat(resDto.getType()).isEqualTo(company.getType());
        assertThat(resDto.getHubId()).isEqualTo(company.getHubId());
        assertThat(resDto.getAddress()).isEqualTo(company.getAddress());
    }

    @Test
    @DisplayName("getCompany_존재하지않거나삭제된업체일때_예외발생")
    void getCompany_존재하지않거나삭제된업체일때_예외발생() {
        // 삭제된 업체는 리포지토리 쿼리 조건(deletedAt IS NULL)에서 이미 걸러지므로 미존재와 동일하게 빈 값이 반환된다.
        UUID companyId = UUID.randomUUID();

        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getCompany(companyId))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
    }

    @Test
    @DisplayName("getCompanyInternal_삭제되지않은업체일때_업체정보를반환한다")
    void getCompanyInternal_삭제되지않은업체일때_업체정보를반환한다() {
        Company company = new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));

        ResGetCompanyInternalDto resDto = companyService.getCompanyInternal(company.getId());

        assertThat(resDto.getCompanyId()).isEqualTo(company.getId());
        assertThat(resDto.getName()).isEqualTo(company.getName());
        assertThat(resDto.getType()).isEqualTo(company.getType());
        assertThat(resDto.getHubId()).isEqualTo(company.getHubId());
        assertThat(resDto.getAddress()).isEqualTo(company.getAddress());
        assertThat(resDto.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("getCompanyInternal_존재하지않거나삭제된업체일때_예외발생")
    void getCompanyInternal_존재하지않거나삭제된업체일때_예외발생() {
        // 삭제된 업체는 리포지토리 쿼리 조건(deletedAt IS NULL)에서 이미 걸러지므로 미존재와 동일하게 빈 값이 반환된다.
        UUID companyId = UUID.randomUUID();

        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getCompanyInternal(companyId))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
    }

    @Test
    @DisplayName("updateCompany_존재하지않거나삭제된업체일때_예외발생")
    void updateCompany_존재하지않거나삭제된업체일때_예외발생() {
        UUID companyId = UUID.randomUUID();
        ReqUpdateCompanyDto reqDto = ReqUpdateCompanyDto.builder().name("삼성전자 물류센터").build();

        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.updateCompany(companyId, reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);

        verify(companyRepository, never()).saveAndFlush(any(Company.class));
    }

    @Test
    @DisplayName("updateCompany_MASTER는_소속조회없이제한없이수정가능하다")
    void updateCompany_MASTER는_소속조회없이제한없이수정가능하다() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        ReqUpdateCompanyDto reqDto = ReqUpdateCompanyDto.builder().name("배송센터A-수정").build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameAndAddressAndDeletedAtIsNullAndIdNot(any(), any(), eq(company.getId())))
                .thenReturn(false);
        when(companyRepository.existsByAddressAndDeletedAtIsNullAndIdNot(any(), eq(company.getId())))
                .thenReturn(false);

        ResUpdateCompanyDto resDto = companyService.updateCompany(company.getId(), reqDto, UserRole.MASTER, USERNAME);

        assertThat(resDto.getCompanyId()).isEqualTo(company.getId());
        assertThat(resDto.getName()).isEqualTo("배송센터A-수정");
        verify(companyRepository).saveAndFlush(company);
        verify(userClient, never()).getUserInfo(any());
    }

    @Test
    @DisplayName("updateCompany_HUB_MANAGER가_담당허브가아닌업체를수정하려할때_예외발생")
    void updateCompany_HUB_MANAGER가_담당허브가아닌업체를수정하려할때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        ReqUpdateCompanyDto reqDto = ReqUpdateCompanyDto.builder().name("배송센터A-수정").build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(userClient.getUserInfo(USERNAME)).thenReturn(new UserInfoDto(UUID.randomUUID(), null));

        assertThatThrownBy(() -> companyService.updateCompany(company.getId(), reqDto, UserRole.HUB_MANAGER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(companyRepository, never()).saveAndFlush(any(Company.class));
    }

    @Test
    @DisplayName("updateCompany_소속조회대상사용자를찾을수없을때_예외발생")
    void updateCompany_소속조회대상사용자를찾을수없을때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        ReqUpdateCompanyDto reqDto = ReqUpdateCompanyDto.builder().name("배송센터A-수정").build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(userClient.getUserInfo(USERNAME)).thenThrow(userNotFoundException(USERNAME));

        assertThatThrownBy(() -> companyService.updateCompany(company.getId(), reqDto, UserRole.HUB_MANAGER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(companyRepository, never()).saveAndFlush(any(Company.class));
    }

    @Test
    @DisplayName("updateCompany_HUB_MANAGER가_소속허브가없을때_예외발생")
    void updateCompany_HUB_MANAGER가_소속허브가없을때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        ReqUpdateCompanyDto reqDto = ReqUpdateCompanyDto.builder().name("배송센터A-수정").build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(userClient.getUserInfo(USERNAME)).thenReturn(new UserInfoDto(null, null));

        assertThatThrownBy(() -> companyService.updateCompany(company.getId(), reqDto, UserRole.HUB_MANAGER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("updateCompany_HUB_MANAGER가_담당허브업체를수정할때_정상수정된다")
    void updateCompany_HUB_MANAGER가_담당허브업체를수정할때_정상수정된다() {
        UUID hubId = UUID.randomUUID();
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, hubId, "서울시 강남구 테헤란로 1");
        ReqUpdateCompanyDto reqDto = ReqUpdateCompanyDto.builder().address("서울시 송파구 올림픽로 1").build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(userClient.getUserInfo(USERNAME)).thenReturn(new UserInfoDto(hubId, null));
        when(companyRepository.existsByNameAndAddressAndDeletedAtIsNullAndIdNot(any(), any(), eq(company.getId())))
                .thenReturn(false);
        when(companyRepository.existsByAddressAndDeletedAtIsNullAndIdNot(any(), eq(company.getId())))
                .thenReturn(false);

        ResUpdateCompanyDto resDto = companyService.updateCompany(company.getId(), reqDto, UserRole.HUB_MANAGER, USERNAME);

        assertThat(resDto.getAddress()).isEqualTo("서울시 송파구 올림픽로 1");
    }

    @Test
    @DisplayName("updateCompany_COMPANY_MANAGER가_본인업체가아닌업체를수정하려할때_예외발생")
    void updateCompany_COMPANY_MANAGER가_본인업체가아닌업체를수정하려할때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        ReqUpdateCompanyDto reqDto = ReqUpdateCompanyDto.builder().name("배송센터A-수정").build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(userClient.getUserInfo(USERNAME)).thenReturn(new UserInfoDto(null, UUID.randomUUID()));

        assertThatThrownBy(() -> companyService.updateCompany(company.getId(), reqDto, UserRole.COMPANY_MANAGER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(companyRepository, never()).saveAndFlush(any(Company.class));
    }

    @Test
    @DisplayName("updateCompany_COMPANY_MANAGER가_본인업체를수정할때_정상수정된다")
    void updateCompany_COMPANY_MANAGER가_본인업체를수정할때_정상수정된다() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        ReqUpdateCompanyDto reqDto = ReqUpdateCompanyDto.builder().type(CompanyType.RECEIVER).build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(userClient.getUserInfo(USERNAME)).thenReturn(new UserInfoDto(null, company.getId()));
        when(companyRepository.existsByNameAndAddressAndDeletedAtIsNullAndIdNot(any(), any(), eq(company.getId())))
                .thenReturn(false);
        when(companyRepository.existsByAddressAndDeletedAtIsNullAndIdNot(any(), eq(company.getId())))
                .thenReturn(false);

        ResUpdateCompanyDto resDto =
                companyService.updateCompany(company.getId(), reqDto, UserRole.COMPANY_MANAGER, USERNAME);

        assertThat(resDto.getType()).isEqualTo(CompanyType.RECEIVER);
    }

    @Test
    @DisplayName("updateCompany_변경할hubId가존재하지않을때_예외발생")
    void updateCompany_변경할hubId가존재하지않을때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        UUID newHubId = UUID.randomUUID();
        ReqUpdateCompanyDto reqDto = ReqUpdateCompanyDto.builder().hubId(newHubId).build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(newHubId)).thenThrow(hubNotFoundException(newHubId));

        assertThatThrownBy(() -> companyService.updateCompany(company.getId(), reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.HUB_NOT_FOUND);

        verify(companyRepository, never()).saveAndFlush(any(Company.class));
    }

    @Test
    @DisplayName("updateCompany_이름과주소가모두같은다른업체가있을때_예외발생")
    void updateCompany_이름과주소가모두같은다른업체가있을때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        ReqUpdateCompanyDto reqDto =
                ReqUpdateCompanyDto.builder().name("배송센터B").address("서울시 송파구 올림픽로 1").build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameAndAddressAndDeletedAtIsNullAndIdNot(
                "배송센터B", "서울시 송파구 올림픽로 1", company.getId())).thenReturn(true);

        assertThatThrownBy(() -> companyService.updateCompany(company.getId(), reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_DUPLICATED);

        verify(companyRepository, never()).saveAndFlush(any(Company.class));
    }

    @Test
    @DisplayName("updateCompany_주소만같은다른업체가있을때_예외발생")
    void updateCompany_주소만같은다른업체가있을때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        ReqUpdateCompanyDto reqDto = ReqUpdateCompanyDto.builder().address("서울시 송파구 올림픽로 1").build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameAndAddressAndDeletedAtIsNullAndIdNot(
                "배송센터A", "서울시 송파구 올림픽로 1", company.getId())).thenReturn(false);
        when(companyRepository.existsByAddressAndDeletedAtIsNullAndIdNot(
                "서울시 송파구 올림픽로 1", company.getId())).thenReturn(true);

        assertThatThrownBy(() -> companyService.updateCompany(company.getId(), reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_ADDRESS_DUPLICATED);

        verify(companyRepository, never()).saveAndFlush(any(Company.class));
    }

    @Test
    @DisplayName("updateCompany_일부필드만요청에포함될때_포함되지않은필드는유지된다")
    void updateCompany_일부필드만요청에포함될때_포함되지않은필드는유지된다() {
        UUID hubId = UUID.randomUUID();
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, hubId, "서울시 강남구 테헤란로 1");
        ReqUpdateCompanyDto reqDto = ReqUpdateCompanyDto.builder().name("배송센터A-수정").build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameAndAddressAndDeletedAtIsNullAndIdNot(any(), any(), eq(company.getId())))
                .thenReturn(false);
        when(companyRepository.existsByAddressAndDeletedAtIsNullAndIdNot(any(), eq(company.getId())))
                .thenReturn(false);

        ResUpdateCompanyDto resDto = companyService.updateCompany(company.getId(), reqDto, UserRole.MASTER, USERNAME);

        assertThat(resDto.getName()).isEqualTo("배송센터A-수정");
        assertThat(resDto.getType()).isEqualTo(CompanyType.PRODUCER);
        assertThat(resDto.getHubId()).isEqualTo(hubId);
        assertThat(resDto.getAddress()).isEqualTo("서울시 강남구 테헤란로 1");
        verify(hubClient, never()).getHub(any());
    }

    @Test
    @DisplayName("updateCompany_저장시점에이름주소유니크제약조건위반이발생할때_예외발생")
    void updateCompany_저장시점에이름주소유니크제약조건위반이발생할때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        ReqUpdateCompanyDto reqDto = ReqUpdateCompanyDto.builder().name("배송센터A-수정").build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameAndAddressAndDeletedAtIsNullAndIdNot(any(), any(), eq(company.getId())))
                .thenReturn(false);
        when(companyRepository.existsByAddressAndDeletedAtIsNullAndIdNot(any(), eq(company.getId())))
                .thenReturn(false);
        doThrow(duplicateKeyException("ux_p_companies_name_address_active"))
                .when(companyRepository).saveAndFlush(any(Company.class));

        assertThatThrownBy(() -> companyService.updateCompany(company.getId(), reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_DUPLICATED);
    }

    private Company companyWithId(String name, CompanyType type, UUID hubId, String address) {
        Company company = new Company(name, type, hubId, address);
        ReflectionTestUtils.setField(company, "id", UUID.randomUUID());
        return company;
    }

    private DataIntegrityViolationException duplicateKeyException(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException(
                "could not execute statement",
                new SQLException("duplicate key value violates unique constraint \"" + constraintName + "\""),
                constraintName
        );
        return new DataIntegrityViolationException("duplicate key", cause);
    }

    private FeignException.NotFound hubNotFoundException(UUID hubId) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://hub-service/api/internal/hubs/" + hubId,
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null
        );
        return new FeignException.NotFound("hub not found", request, null, Collections.emptyMap());
    }
}