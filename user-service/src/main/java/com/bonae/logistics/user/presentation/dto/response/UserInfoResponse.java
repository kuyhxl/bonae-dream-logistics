package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoResponse {
    private String username;
    private String name;
    private String slackId;
    private Role role;

    public UserInfoResponse(User user) {
        this.username = user.getUsername();
        this.name = user.getName();
        this.slackId = user.getSlackId();
        this.role = user.getRole();
    }
}
