package com.bonae.logistics.user.presentation.dto.request;

import com.bonae.logistics.user.domain.entity.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserApprovalRequest {

    @NotNull(message = "처리 구분(APPROVED / REJECTED)은 필수입니다.")
    private ApprovalStatus approvalStatus;

    // 아래 값들은 approvalStatus에 따라 필요 여부가 달라져 서비스에서 교차 검증한다.
    private Role role;
    private UUID hubId;
    private UUID companyId;

    @Size(max = 255, message = "거절 사유는 255자를 초과할 수 없습니다.")
    private String rejectReason;
}