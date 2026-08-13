package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

// 사용자 상세. password는 어떤 경우에도 노출하지 않는다.
@Getter
@Builder
@Schema(description = "사용자 상세 응답. 비밀번호는 어떤 경우에도 포함하지 않는다.")
public class UserDetailResponse {

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

    @Schema(description = "가입을 처리한 관리자의 아이디", example = "master01")
    private final String approvedBy;

    @Schema(description = "가입 처리 일시", example = "2026-08-13T11:20:00")
    private final LocalDateTime approvedAt;

    @Schema(description = "가입 신청 일시", example = "2026-08-13T10:15:30")
    private final LocalDateTime createdAt;

    @Schema(description = "최종 수정 일시", example = "2026-08-13T12:00:00")
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
