package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "(내부) 사용자 정보 응답. 승인되고 삭제되지 않은 사용자만 조회된다.")
public class UserInfoResponse {

    @Schema(description = "사용자 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private UUID id;

    @Schema(description = "아이디", example = "hyerim01")
    private String username;

    @Schema(description = "이름", example = "김혜림")
    private String name;

    @Schema(description = "슬랙 ID. 알림 발송 대상 식별에 사용한다.", example = "U01ABCDEF")
    private String slackId;

    @Schema(description = "역할", example = "HUB_MANAGER")
    private Role role;

    @Schema(description = "소속 허브 ID. 소속이 업체이거나 MASTER면 null이다.", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private UUID hubId;

    @Schema(description = "소속 업체 ID. 소속이 허브이거나 MASTER면 null이다.", example = "9f2a1c40-52c1-49f0-9b3e-1a2f5c7d8e90")
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
