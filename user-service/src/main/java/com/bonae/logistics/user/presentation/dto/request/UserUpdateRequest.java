package com.bonae.logistics.user.presentation.dto.request;

import com.bonae.logistics.user.domain.entity.Role;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// PATCH 부분 수정. null인 필드는 "변경 없음"으로 취급한다.
// role에 정의되지 않은 값이 오면 Jackson이 HttpMessageNotReadableException을 던져 400으로 처리된다.
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