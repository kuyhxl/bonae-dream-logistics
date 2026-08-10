package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserSummaryResponse {

    private final UUID userId;
    private final String username;
    private final String name;
    private final String slackId;
    private final Role role; // 승인 전이면 null
    private final Status status;
    private final String affiliationName;
    private final UUID hubId;
    private final UUID companyId;
    private final LocalDateTime createdAt;

    public static UserSummaryResponse from(User user) {
        return UserSummaryResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .slackId(user.getSlackId())
                .role(user.getRole())
                .status(user.getStatus())
                .affiliationName(user.getAffiliationName())
                .hubId(user.getHubId())
                .companyId(user.getCompanyId())
                .createdAt(user.getCreatedAt())
                .build();
    }
}