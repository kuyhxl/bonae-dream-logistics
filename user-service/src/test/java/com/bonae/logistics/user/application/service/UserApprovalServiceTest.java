package com.bonae.logistics.user.application.service;

import com.bonae.logistics.common.auth.CurrentAuditorProvider;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import com.bonae.logistics.user.domain.repository.UserRepository;
import com.bonae.logistics.user.infrastructure.client.CompanyClient;
import com.bonae.logistics.user.infrastructure.client.CompanyInfoDto;
import com.bonae.logistics.user.infrastructure.client.HubClient;
import com.bonae.logistics.user.presentation.dto.request.ApprovalStatus;
import com.bonae.logistics.user.presentation.dto.request.UserApprovalRequest;
import com.bonae.logistics.user.presentation.dto.response.UserApprovalResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserApprovalService 가입 승인/거절")
class UserApprovalServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID HUB_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final String MASTER = "master01";

    @InjectMocks
    private UserApprovalService userApprovalService;

    @Mock private UserRepository userRepository;
    @Mock private HubClient hubClient;
    @Mock private CompanyClient companyClient;
    @Mock private CurrentAuditorProvider currentAuditorProvider;
    @Mock private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        given(currentAuditorProvider.getCurrentAuditorOrSystem()).willReturn(MASTER);
        // TransactionTemplate 목은 콜백을 그대로 실행시켜 준다.
        given(transactionTemplate.execute(any()))
                .willAnswer(i -> i.<TransactionCallback<Object>>getArgument(0).doInTransaction(null));
    }

    private User pendingUser() {
        return User.builder()
                .username("testuser")
                .password("encoded")
                .name("테스트")
                .slackId("U000TEST123")
                .affiliationName("서울허브")
                .build();
    }

    @Test
    @DisplayName("허브 관리자로 승인하면 status·role·hubId·승인자가 확정된다")
    void approve_hubManager_success() {
        User user = pendingUser();
        given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(user));

        UserApprovalResponse response = userApprovalService.process(USER_ID,
                new UserApprovalRequest(ApprovalStatus.APPROVED, Role.HUB_MANAGER, HUB_ID, null, null),
                Role.MASTER, null);

        assertThat(response.getStatus()).isEqualTo(Status.APPROVED);
        assertThat(response.getRole()).isEqualTo(Role.HUB_MANAGER);
        assertThat(response.getHubId()).isEqualTo(HUB_ID);
        assertThat(response.getApprovedBy()).isEqualTo(MASTER);
        assertThat(response.getApprovedAt()).isNotNull();
        then(hubClient).should().validateHubExists(HUB_ID);
        then(companyClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("거절하면 role·소속은 NULL로 유지된다")
    void reject_keepsRoleNull() {
        User user = pendingUser();
        given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(user));

        UserApprovalResponse response = userApprovalService.process(USER_ID,
                new UserApprovalRequest(ApprovalStatus.REJECTED, null, null, null, "소속 확인 불가"),
                Role.HUB_MANAGER, HUB_ID);

        assertThat(response.getStatus()).isEqualTo(Status.REJECTED);
        assertThat(response.getRole()).isNull();
        assertThat(response.getHubId()).isNull();
        assertThat(response.getCompanyId()).isNull();
        // 거절은 소속 확정이 없으므로 외부 서비스를 호출하지 않는다.
        then(hubClient).should(never()).validateHubExists(any());
        then(companyClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("MASTER 권한은 승인 API로 부여할 수 없다")
    void approve_masterRole_rejected() {
        assertThatThrownBy(() -> userApprovalService.process(USER_ID,
                new UserApprovalRequest(ApprovalStatus.APPROVED, Role.MASTER, HUB_ID, null, null),
                Role.MASTER, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MASTER_ROLE_NOT_ASSIGNABLE);

        then(userRepository).should(never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("배송 담당자는 hubId 없이도 승인된다 (허브 간 이동 담당)")
    void approve_deliveryManager_withoutHub() {
        User user = pendingUser();
        given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(user));

        UserApprovalResponse response = userApprovalService.process(USER_ID,
                new UserApprovalRequest(ApprovalStatus.APPROVED, Role.DELIVERY_MANAGER, null, null, null),
                Role.MASTER, null);

        assertThat(response.getRole()).isEqualTo(Role.DELIVERY_MANAGER);
        assertThat(response.getHubId()).isNull();
        // 소속 허브가 없는 유형이므로 허브 존재 검증을 호출하지 않는다.
        then(hubClient).shouldHaveNoInteractions();
        then(companyClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("허브 관리자 승인에 hubId가 없으면 400으로 거절한다")
    void approve_hubManager_withoutHub() {
        assertThatThrownBy(() -> userApprovalService.process(USER_ID,
                new UserApprovalRequest(ApprovalStatus.APPROVED, Role.HUB_MANAGER, null, null, null),
                Role.MASTER, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_AFFILIATION);

        then(hubClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("허브 관리자는 자기 허브 소속으로만 승인할 수 있다")
    void approve_hubManager_ownHub() {
        User user = pendingUser();
        given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(user));

        UserApprovalResponse response = userApprovalService.process(USER_ID,
                new UserApprovalRequest(ApprovalStatus.APPROVED, Role.HUB_MANAGER, HUB_ID, null, null),
                Role.HUB_MANAGER, HUB_ID);

        assertThat(response.getHubId()).isEqualTo(HUB_ID);
    }

    @Test
    @DisplayName("허브 관리자가 다른 허브 소속으로 승인하면 403으로 거절한다")
    void approve_hubManager_otherHub_forbidden() {
        UUID otherHubId = UUID.randomUUID();

        assertThatThrownBy(() -> userApprovalService.process(USER_ID,
                new UserApprovalRequest(ApprovalStatus.APPROVED, Role.HUB_MANAGER, otherHubId, null, null),
                Role.HUB_MANAGER, HUB_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

        then(userRepository).should(never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("허브 간 이동 담당(hubId 없음)은 허브 관리자가 승인할 수 없다")
    void approve_deliveryManager_withoutHub_byHubManager_forbidden() {
        assertThatThrownBy(() -> userApprovalService.process(USER_ID,
                new UserApprovalRequest(ApprovalStatus.APPROVED, Role.DELIVERY_MANAGER, null, null, null),
                Role.HUB_MANAGER, HUB_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("허브 관리자는 자기 허브 소속 업체의 담당자만 승인할 수 있다")
    void approve_companyManager_otherHub_forbidden() {
        UUID otherHubId = UUID.randomUUID();
        given(companyClient.getCompany(COMPANY_ID)).willReturn(new CompanyInfoDto(COMPANY_ID, otherHubId));

        assertThatThrownBy(() -> userApprovalService.process(USER_ID,
                new UserApprovalRequest(ApprovalStatus.APPROVED, Role.COMPANY_MANAGER, null, COMPANY_ID, null),
                Role.HUB_MANAGER, HUB_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("이미 처리된 가입 요청은 409로 거절한다")
    void approve_alreadyProcessed() {
        User user = pendingUser();
        user.approve(Role.COMPANY_MANAGER, null, COMPANY_ID, MASTER);
        given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userApprovalService.process(USER_ID,
                new UserApprovalRequest(ApprovalStatus.APPROVED, Role.HUB_MANAGER, HUB_ID, null, null),
                Role.MASTER, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ALREADY_PROCESSED);
    }
}