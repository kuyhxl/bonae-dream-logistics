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
     * null/공백은 "변경 없음"이므로 건드리지 않는다.
     */
    public void update(String name, String slackId, Role role, UUID hubId, UUID companyId) {
        // 명세 "권한·소속 확정 정책": MASTER는 초기 데이터로만 만들고 API로는 부여하지 않는다.
        if (role == Role.MASTER) {
            throw new BusinessException(ErrorCode.MASTER_ROLE_NOT_ASSIGNABLE);
        }
        // 사용자는 허브 소속이거나 업체 소속이지 둘 다일 수는 없다.
        if (hubId != null && companyId != null) {
            throw new BusinessException(ErrorCode.INVALID_AFFILIATION);
        }

        if (StringUtils.hasText(name)) {
            this.name = name;
        }
        if (StringUtils.hasText(slackId)) {
            this.slackId = slackId;
        }
        if (role != null) {
            this.role = role;
        }
        // 한쪽 소속을 지정하면 반대쪽은 비운다. 허브 담당자 -> 업체 담당자 이동 대비.
        if (hubId != null) {
            this.hubId = hubId;
            this.companyId = null;
        }
        if (companyId != null) {
            this.companyId = companyId;
            this.hubId = null;
        }
    }
}
