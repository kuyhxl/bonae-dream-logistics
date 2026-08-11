package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoResponse {
    private UUID id;
    private String username;
    private String name;
    private String slackId;
    private Role role;
    private UUID hubId;
    private UUID companyId;

    public UserInfoResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.slackId = user.getSlackId();
        this.role = user.getRole();
        this.hubId = user.getHubId();
        this.companyId = user.getCompanyId();
    }
}
