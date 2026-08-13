package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "(내부) 배송 담당자 응답")
public class DeliveryManagerResponse {

    @Schema(description = "사용자 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private UUID userId;

    @Schema(description = "아이디", example = "hyerim01")
    private String userName;

    @Schema(description = "이름", example = "김혜림")
    private String name;

    @Schema(description = "슬랙 ID. 배송 배정 알림 발송에 사용한다.", example = "U01ABCDEF")
    private String slackId;

    @Schema(description = "소속 허브 ID. 허브 간 이동 담당자는 소속 허브가 없어 null이다.", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private UUID hubId; // 소속 허브 (허브 간 이동 담당자는 null)

    public DeliveryManagerResponse(User user) {
        this.userId = user.getId();
        this.userName = user.getUsername();
        this.name = user.getName();
        this.slackId = user.getSlackId();
        this.hubId = user.getHubId();
    }
}
