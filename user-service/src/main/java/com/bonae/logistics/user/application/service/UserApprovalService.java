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
        // 권한 미지정은 입력 오류(400), MASTER 지정은 권한 상승 시도(403)로 구분한다.
        if (role == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (!ASSIGNABLE_ROLES.contains(role)) {
            throw new BusinessException(ErrorCode.MASTER_ROLE_NOT_ASSIGNABLE);
        }
        return role;
    }

    // 역할과 소속 조합의 최종 검증은 User.approve() 안의 validateAffiliation()이 담당한다.
    // 여기서는 외부 서비스를 호출하는 데 필요한 값이 있는지 확인하고, 실제 존재 여부만 검증한다.
    // 원격의 HUB_NOT_FOUND / COMPANY_NOT_FOUND는 common의 FeignErrorDecoder가
    // BusinessException으로 복원해 그대로 전파하므로 별도 예외 변환을 하지 않는다.
    private void validateAffiliation(Role role, UUID hubId, UUID companyId) {
        switch (role) {
            case HUB_MANAGER -> {
                if (hubId == null) {
                    throw new BusinessException(ErrorCode.INVALID_AFFILIATION);
                }
                hubClient.validateHubExists(hubId);
            }
            case COMPANY_MANAGER -> {
                if (companyId == null) {
                    throw new BusinessException(ErrorCode.INVALID_AFFILIATION);
                }
                companyClient.validateCompanyExists(companyId);
            }
            // 배송 담당자의 hubId는 담당 유형을 구분하는 값이라 null도 정상이다.
            // (null = 허브 간 이동 담당, 값 있음 = 업체 배송 담당 — DeliveryManagerType 참고)
            case DELIVERY_MANAGER -> {
                if (hubId != null) {
                    hubClient.validateHubExists(hubId);
                }
            }
            default -> throw new BusinessException(ErrorCode.MASTER_ROLE_NOT_ASSIGNABLE);
        }
    }
}