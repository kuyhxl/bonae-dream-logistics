package com.bonae.logistics.user.presentation.dto.request;

import com.bonae.logistics.user.domain.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "가입 승인 · 거절 요청. 승인 시에는 role과 소속(hubId 또는 companyId)이 함께 필요하다.")
public class UserApprovalRequest {

    @NotNull(message = "처리 구분(APPROVED / REJECTED)은 필수입니다.")
    @Schema(description = "처리 구분", example = "APPROVED", requiredMode = Schema.RequiredMode.REQUIRED)
    private ApprovalStatus approvalStatus;

    // 아래 값들은 approvalStatus에 따라 필요 여부가 달라져 서비스에서 교차 검증한다.
    @Schema(description = "승인 시 부여할 역할. MASTER는 부여할 수 없다.",
            allowableValues = {"HUB_MANAGER", "DELIVERY_MANAGER", "COMPANY_MANAGER"}, example = "HUB_MANAGER")
    private Role role;

    @Schema(description = "소속 허브 ID. 허브 관리자·배송 담당자를 승인할 때 지정한다.", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private UUID hubId;

    @Schema(description = "소속 업체 ID. 업체 담당자를 승인할 때 지정한다.", example = "9f2a1c40-52c1-49f0-9b3e-1a2f5c7d8e90")
    private UUID companyId;

    @Size(max = 255, message = "거절 사유는 255자를 초과할 수 없습니다.")
    @Schema(description = "거절 사유. 거절할 때만 사용하며 255자를 넘을 수 없다.", example = "소속 정보가 확인되지 않았습니다.")
    private String rejectReason;
}
