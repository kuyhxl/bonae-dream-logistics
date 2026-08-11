package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

// 사용자 상세. password는 어떤 경우에도 노출하지 않는다.
@Getter
@Builder
public class UserDetailResponse {

    private final UUID userId;
    private final String username;
    private final String name;
    private final String slackId;
    private final Role role; // 승인 전이면 null
    private final Status status;
    private final String affiliationName;
    private final UUID hubId;
    private final UUID companyId;
    private final String approvedBy;
    private final LocalDateTime approvedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static UserDetailResponse from(User user) {
        return UserDetailResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .slackId(user.getSlackId())
                .role(user.getRole())
                .status(user.getStatus())
                .affiliationName(user.getAffiliationName())
                .hubId(user.getHubId())
                .companyId(user.getCompanyId())
                .approvedBy(user.getApprovedBy())
                .approvedAt(user.getApprovedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}