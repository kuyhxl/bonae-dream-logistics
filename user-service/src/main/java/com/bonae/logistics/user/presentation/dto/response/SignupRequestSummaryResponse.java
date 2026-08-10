package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

// 가입 요청 목록 한 건. 승인 판단에 필요한 소속명과 처리 이력을 함께 노출한다.
@Getter
@Builder
public class SignupRequestSummaryResponse {

    private final UUID userId;
    private final String username;
    private final String name;
    private final String slackId;
    private final String affiliationName;   // 신청자가 자유 입력한 소속명
    private final Status status;
    private final Role role;                // 승인 전이면 null
    private final UUID hubId;
    private final UUID companyId;
    private final String approvedBy;
    private final LocalDateTime approvedAt;
    private final LocalDateTime requestedAt; // = createdAt

    public static SignupRequestSummaryResponse from(User user) {
        return SignupRequestSummaryResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .slackId(user.getSlackId())
                .affiliationName(user.getAffiliationName())
                .status(user.getStatus())
                .role(user.getRole())
                .hubId(user.getHubId())
                .companyId(user.getCompanyId())
                .approvedBy(user.getApprovedBy())
                .approvedAt(user.getApprovedAt())
                .requestedAt(user.getCreatedAt())
                .build();
    }
}