package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

// 가입 요청 목록 한 건. 승인 판단에 필요한 소속명과 처리 이력을 함께 노출한다.
// UserSummaryResponse를 재사용하지 않는 이유: approvedBy/approvedAt이 필요하다.
@Getter
@Builder
@Schema(description = "가입 요청 목록 항목. 승인 판단에 필요한 소속명과 처리 이력을 함께 담는다.")
public class SignupRequestSummaryResponse {

    @Schema(description = "사용자 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private final UUID userId;

    @Schema(description = "아이디", example = "hyerim01")
    private final String username;

    @Schema(description = "이름", example = "김혜림")
    private final String name;

    @Schema(description = "슬랙 ID", example = "U01ABCDEF")
    private final String slackId;

    @Schema(description = "신청자가 자유롭게 입력한 소속명. 승인 시 관리자가 실제 허브/업체와 연결한다.", example = "서울특별시 센터")
    private final String affiliationName;   // 신청자가 자유 입력한 소속명

    @Schema(description = "가입 상태", example = "PENDING")
    private final Status status;

    @Schema(description = "부여된 역할. 승인 전이면 null이다.", example = "HUB_MANAGER")
    private final Role role;                // 승인 전이면 null

    @Schema(description = "연결된 허브 ID. 승인 전이거나 업체 소속이면 null이다.", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private final UUID hubId;

    @Schema(description = "연결된 업체 ID. 승인 전이거나 허브 소속이면 null이다.", example = "9f2a1c40-52c1-49f0-9b3e-1a2f5c7d8e90")
    private final UUID companyId;

    @Schema(description = "처리한 관리자의 아이디. 미처리 건이면 null이다.", example = "master01")
    private final String approvedBy;

    @Schema(description = "처리 일시. 미처리 건이면 null이다.", example = "2026-08-13T11:20:00")
    private final LocalDateTime approvedAt;

    @Schema(description = "가입 신청 일시", example = "2026-08-13T10:15:30")
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
