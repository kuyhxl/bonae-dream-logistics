package com.bonae.logistics.user.domain.entity;

import com.bonae.logistics.common.entity.BaseEntity;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_users", schema = "user_service")
public class User extends BaseEntity {
    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "username", unique = true, length = 10, nullable = false)
    private String username;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slack_id", nullable = false, length = 100)
    private String slackId;

    @Column(name = "role", length = 20)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(name = "affiliation_name", nullable = false, length = 100)
    private String affiliationName;

    @Column(name = "hub_id")
    private UUID hubId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "approved_by", length = 10)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /*
     * 마스터 관리자에 의한 사용자 정보 수정.
     * name, slackId는 값이 있을 때만 바꾼다.
     *
     * role을 넘기면 소속(hubId, companyId)까지 함께 재확정한다.
     * 역할이 바뀌면 이전 소속은 더 이상 유효하지 않으므로, 요청에 없는 소속은 null로 비운다.
     * 소속만 바꾸려면 role을 생략한다.
     *
     * 어느 경로로 들어오든 마지막에 역할과 소속의 최종 상태를 함께 검증한다.
     */
    public void update(String name, String slackId, Role role, UUID hubId, UUID companyId) {
        // 명세 "권한·소속 확정 정책": MASTER는 초기 데이터로만 만들고 API로는 부여하지 않는다.
        if (role == Role.MASTER) {
            throw new BusinessException(ErrorCode.MASTER_ROLE_NOT_ASSIGNABLE);
        }

        if (StringUtils.hasText(name)) {
            this.name = name;
        }
        if (StringUtils.hasText(slackId)) {
            this.slackId = slackId;
        }

        if (role != null) {
            // 역할 변경은 소속 재확정을 포함한다. 요청에 없는 소속은 비운다.
            this.role = role;
            this.hubId = hubId;
            this.companyId = companyId;
        } else {
            // 역할은 그대로 두고 소속만 부분 수정한다.
            if (hubId != null) {
                this.hubId = hubId;
            }
            if (companyId != null) {
                this.companyId = companyId;
            }
        }

        validateAffiliation();
    }

    /*
     * 역할별 소속 규칙을 최종 상태 기준으로 검증한다.
     * 배송 담당자의 hubId는 담당 유형을 구분하는 값이라 null도 정상이다.
     * (null = 허브 간 이동 담당, 값 있음 = 업체 배송 담당 — DeliveryManagerType 참고)
     */
    private void validateAffiliation() {
        // 승인 전(PENDING) 사용자는 역할도 소속도 확정되지 않은 상태여야 한다.
        if (this.role == null) {
            requireValidAffiliation(this.hubId == null && this.companyId == null);
            return;
        }

        switch (this.role) {
            case MASTER -> requireValidAffiliation(this.hubId == null && this.companyId == null);
            case HUB_MANAGER -> requireValidAffiliation(this.hubId != null && this.companyId == null);
            case COMPANY_MANAGER -> requireValidAffiliation(this.companyId != null && this.hubId == null);
            case DELIVERY_MANAGER -> requireValidAffiliation(this.companyId == null);
        }
    }

    private void requireValidAffiliation(boolean valid) {
        if (!valid) {
            throw new BusinessException(ErrorCode.INVALID_AFFILIATION);
        }
    }
}
