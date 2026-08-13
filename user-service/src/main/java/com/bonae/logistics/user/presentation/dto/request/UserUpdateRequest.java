package com.bonae.logistics.user.presentation.dto.request;

import com.bonae.logistics.user.domain.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// PATCH 부분 수정. null인 필드는 "변경 없음"으로 취급한다.
// UserSearchCondition과 달리 body는 Jackson이 처리하므로, 잘못된 role 값은
// HttpMessageNotReadableException -> 400 INVALID_INPUT으로 이미 걸러진다.
@Getter
@Setter
@Schema(description = "사용자 수정 요청. 전달하지 않은 필드는 변경하지 않는다.")
public class UserUpdateRequest {

    @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다.")
    @Schema(description = "이름", example = "김혜림")
    private String name;

    @Size(max = 100, message = "슬랙 ID는 100자를 초과할 수 없습니다.")
    @Schema(description = "슬랙 ID. 배송 알림 발송에 사용한다.", example = "U01ABCDEF")
    private String slackId;

    @Schema(description = "역할", allowableValues = {"MASTER", "HUB_MANAGER", "DELIVERY_MANAGER", "COMPANY_MANAGER"}, example = "HUB_MANAGER")
    private Role role;

    @Schema(description = "소속 허브 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private UUID hubId;

    @Schema(description = "소속 업체 ID", example = "9f2a1c40-52c1-49f0-9b3e-1a2f5c7d8e90")
    private UUID companyId;
}
