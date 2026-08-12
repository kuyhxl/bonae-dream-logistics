package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
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
public class UserApprovalResponse {

    private UUID userId;
    private Status status;
    private Role role;
    private UUID hubId;
    private UUID companyId;
    private String approvedBy;
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