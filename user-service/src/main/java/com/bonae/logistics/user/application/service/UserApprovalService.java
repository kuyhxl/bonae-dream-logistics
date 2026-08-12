package com.bonae.logistics.user.application.service;

import com.bonae.logistics.common.auth.CurrentAuditorProvider;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.User;
import com.bonae.logistics.user.domain.repository.UserRepository;
import com.bonae.logistics.user.infrastructure.client.CompanyClient;
import com.bonae.logistics.user.infrastructure.client.CompanyInfoDto;
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
    public UserApprovalResponse process(UUID userId, UserApprovalRequest request,
                                        Role requesterRole, UUID requesterHubId) {
        String processedBy = currentAuditorProvider.getCurrentAuditorOrSystem();

        // 거절은 소속을 확정하지 않으므로 허브 범위 제한을 두지 않는다. MASTER·HUB_MANAGER 모두 가능하다.
        if (request.getApprovalStatus() == ApprovalStatus.REJECTED) {
            return transactionTemplate.execute(status -> {
                User user = findTarget(userId);
                user.reject(processedBy, request.getRejectReason());
                return new UserApprovalResponse(user);
            });
        }

        Role role = validateAssignableRole(request.getRole());
        validateAffiliationAndScope(role, request.getHubId(), request.getCompanyId(), requesterRole, requesterHubId);

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

    /*
     * 역할과 소속 조합의 최종 검증은 User.approve() 안의 validateAffiliation()이 담당한다.
     * 여기서는 외부 서비스 호출에 필요한 값이 있는지 확인하고, 소속의 실제 존재 여부와
     * 요청자의 승인 범위를 함께 검증한다. (업체 조회 응답을 두 용도로 재사용하기 위해 한 메서드로 묶었다)
     *
     * 원격의 HUB_NOT_FOUND / COMPANY_NOT_FOUND는 common의 FeignErrorDecoder가
     * BusinessException으로 복원해 그대로 전파하므로 별도 예외 변환을 하지 않는다.
     */
    private void validateAffiliationAndScope(Role role, UUID hubId, UUID companyId,
                                             Role requesterRole, UUID requesterHubId) {
        switch (role) {
            case HUB_MANAGER -> {
                if (hubId == null) {
                    throw new BusinessException(ErrorCode.INVALID_AFFILIATION);
                }
                hubClient.validateHubExists(hubId);
                requireOwnHub(requesterRole, requesterHubId, hubId);
            }
            case COMPANY_MANAGER -> {
                if (companyId == null) {
                    throw new BusinessException(ErrorCode.INVALID_AFFILIATION);
                }
                // 존재 검증과 소속 허브 확인을 한 번의 호출로 처리한다.
                CompanyInfoDto company = companyClient.getCompany(companyId);
                requireOwnHub(requesterRole, requesterHubId, company.hubId());
            }
            // 배송 담당자의 hubId는 담당 유형을 구분하는 값이라 null도 정상이다.
            // (null = 허브 간 이동 담당, 값 있음 = 업체 배송 담당 — DeliveryManagerType 참고)
            case DELIVERY_MANAGER -> {
                if (hubId != null) {
                    hubClient.validateHubExists(hubId);
                }
                // 허브 간 이동 담당(hubId 없음)은 특정 허브에 속하지 않으므로 MASTER만 지정할 수 있다.
                requireOwnHub(requesterRole, requesterHubId, hubId);
            }
            default -> throw new BusinessException(ErrorCode.MASTER_ROLE_NOT_ASSIGNABLE);
        }
    }

    /*
     * 허브 관리자는 자기 허브 소속으로만 승인할 수 있다. MASTER는 제한이 없다.
     *
     * 대기 상태 사용자는 hubId가 NULL이라 "대상자의 허브"로는 범위를 판정할 수 없다.
     * (남은 단서인 affiliationName은 신청자가 자유 입력한 값이라 인가 근거로 쓸 수 없다)
     * 그래서 관리자가 확정하려는 소속이 본인 허브인지를 기준으로 검사한다.
     */
    private void requireOwnHub(Role requesterRole, UUID requesterHubId, UUID targetHubId) {
        if (requesterRole == Role.MASTER) {
            return;
        }
        // 소속 기반 스코프가 필요한데 헤더가 없으면 403 (API 공통 규격 3-1)
        if (requesterHubId == null || !requesterHubId.equals(targetHubId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}