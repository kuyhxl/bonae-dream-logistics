package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryManagerResponse {
    private UUID userId;
    private String userName;
    private String name;
    private String slackId;
    private UUID hubId; // 소속 허브 (허브 간 이동 담당자는 null)

    public DeliveryManagerResponse(User user) {
        this.userId = user.getId();
        this.userName = user.getUsername();
        this.name = user.getName();
        this.slackId = user.getSlackId();
        this.hubId = user.getHubId();
    }
}
