package com.bonae.logistics.user.presentation.dto.request;

import com.bonae.logistics.user.domain.entity.Role;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// PATCH 부분 수정. null인 필드는 "변경 없음"으로 취급한다.
// UserSearchCondition과 달리 body는 Jackson이 처리하므로, 잘못된 role 값은
// HttpMessageNotReadableException -> 400 INVALID_INPUT으로 이미 걸러진다.
@Getter
@Setter
public class UserUpdateRequest {

    @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다.")
    private String name;

    @Size(max = 100, message = "슬랙 ID는 100자를 초과할 수 없습니다.")
    private String slackId;

    private Role role;
    private UUID hubId;
    private UUID companyId;
}