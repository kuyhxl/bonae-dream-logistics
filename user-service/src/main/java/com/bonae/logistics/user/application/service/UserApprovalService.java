package com.bonae.logistics.user.application.service;

import com.bonae.logistics.common.auth.CurrentAuditorProvider;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.User;
import com.bonae.logistics.user.domain.repository.UserRepository;
import com.bonae.logistics.user.infrastructure.client.CompanyClient;
import com.bonae.logistics.user.infrastructure.client.HubClient;
import com.bonae.logistics.user.presentation.dto.request.ApprovalStatus;
import com.bonae.logistics.user.presentation.dto.request.UserApprovalRequest;
import com.bonae.logistics.user.presentation.dto.response.UserApprovalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserApprovalService {

    // MASTER는 승인 API로 부여할 수 없다 (초기 데이터로만 생성).
    private static final Set<Role> ASSIGNABLE_ROLES =
            EnumSet.of(Role.HUB_MANAGER, Role.DELIVERY_MANAGER, Role.COMPANY_MANAGER);

    private final UserRepository userRepository;
    private final HubClient hubClient;
    private final CompanyClient companyClient;
    private final CurrentAuditorProvider currentAuditorProvider;
    private final TransactionTemplate transactionTemplate;

    // @Transactional 미사용: 허브·업체 서비스 호출이 DB 커넥션을 점유하지 않도록 트랜잭션 밖에서 검증한다.
    public UserApprovalResponse process(UUID userId, UserApprovalRequest request) {
        String processedBy = currentAuditorProvider.getCurrentAuditorOrSystem();

        if (request.getApprovalStatus() == ApprovalStatus.REJECTED) {
            return transactionTemplate.execute(status -> {
                User user = findTarget(userId);
                user.reject(processedBy, request.getRejectReason());
                return new UserApprovalResponse(user);
            });
        }

        Role role = validateAssignableRole(request.getRole());
        validateAffiliation(role, request.getHubId(), request.getCompanyId());

        return transactionTemplate.execute(status -> {
            User user = findTarget(userId);
            user.approve(role, request.getHubId(), request.getCompanyId(), processedBy);
            return new UserApprovalResponse(user);
        });
    }

    // 논리 삭제된 사용자는 404로 응답한다. 이미 처리된 요청인지는 엔티티가 검사한다.
    private User findTarget(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Role validateAssignableRole(Role role) {
        if (role == null || !ASSIGNABLE_ROLES.contains(role)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return role;
    }

    // 권한별로 필요한 소속을 강제하고, 실제 존재 여부는 각 서비스 내부 API로 검증한다.
    // 원격의 HUB_NOT_FOUND / COMPANY_NOT_FOUND는 common의 FeignErrorDecoder가
    // BusinessException으로 복원해 그대로 전파하므로 별도 예외 변환을 하지 않는다.
    private void validateAffiliation(Role role, UUID hubId, UUID companyId) {
        if (role == Role.COMPANY_MANAGER) {
            if (companyId == null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            companyClient.validateCompanyExists(companyId);
            return;
        }

        // HUB_MANAGER / DELIVERY_MANAGER
        if (hubId == null || companyId != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        hubClient.validateHubExists(hubId);
    }
}