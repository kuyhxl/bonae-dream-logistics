package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "사용자 목록 항목")
public class UserSummaryResponse {

    @Schema(description = "사용자 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private final UUID userId;

    @Schema(description = "아이디", example = "hyerim01")
    private final String username;

    @Schema(description = "이름", example = "김혜림")
    private final String name;

    @Schema(description = "슬랙 ID", example = "U01ABCDEF")
    private final String slackId;

    @Schema(description = "역할. 승인 전이면 null이다.", example = "HUB_MANAGER")
    private final Role role; // 승인 전이면 null

    @Schema(description = "가입 상태", example = "APPROVED")
    private final Status status;

    @Schema(description = "신청자가 자유롭게 입력한 소속명", example = "서울특별시 센터")
    private final String affiliationName;

    @Schema(description = "소속 허브 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private final UUID hubId;

    @Schema(description = "소속 업체 ID", example = "9f2a1c40-52c1-49f0-9b3e-1a2f5c7d8e90")
    private final UUID companyId;

    @Schema(description = "가입 신청 일시", example = "2026-08-13T10:15:30")
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
