package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "가입 승인 · 거절 처리 결과")
public class UserApprovalResponse {

    @Schema(description = "사용자 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private UUID userId;

    @Schema(description = "처리 후 가입 상태", example = "APPROVED")
    private Status status;

    @Schema(description = "부여된 역할. 거절된 경우 null이다.", example = "HUB_MANAGER")
    private Role role;

    @Schema(description = "연결된 허브 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private UUID hubId;

    @Schema(description = "연결된 업체 ID", example = "9f2a1c40-52c1-49f0-9b3e-1a2f5c7d8e90")
    private UUID companyId;

    @Schema(description = "처리한 관리자의 아이디", example = "master01")
    private String approvedBy;

    @Schema(description = "처리 일시", example = "2026-08-13T11:20:00")
    private LocalDateTime approvedAt;

    public UserApprovalResponse(User user) {
        this.userId = user.getId();
        this.status = user.getStatus();
        this.role = user.getRole();
        this.hubId = user.getHubId();
        this.companyId = user.getCompanyId();
        this.approvedBy = user.getApprovedBy();
        this.approvedAt = user.getApprovedAt();
    }
}
