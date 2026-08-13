package com.bonae.logistics.user.presentation.dto.request;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.UUID;

// 잘못된 값을 그대로 바인딩하면 BindException(-> 500)이 되므로 문자열로 받아 직접 변환한다.
@Getter
@Setter
@Schema(description = "사용자 검색 조건. 생략한 항목은 조건에서 제외되며, 정의되지 않은 값을 보내면 400으로 거절한다.")
public class UserSearchCondition {

    @Schema(description = "아이디 또는 이름 검색어. 공백만 입력하면 조건 없음으로 처리한다.", example = "혜림")
    private String keyword;

    @Schema(description = "역할. 생략하면 전체 조회.",
            allowableValues = {"MASTER", "HUB_MANAGER", "DELIVERY_MANAGER", "COMPANY_MANAGER"}, example = "HUB_MANAGER")
    private String role;

    @Schema(description = "가입 상태. 생략하면 전체 조회.",
            allowableValues = {"PENDING", "APPROVED", "REJECTED"}, example = "APPROVED")
    private String status;

    @Schema(description = "소속 허브 ID. 생략하면 전체 조회.", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private String hubId;

    // 공백만 들어온 검색어는 조건 없음으로 취급한다.
    public String getKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    public Role toRole() {
        return parseEnum(Role.class, role);
    }

    public Status toStatus() {
        return parseEnum(Status.class, status);
    }

    public UUID toHubId() {
        if (!StringUtils.hasText(hubId)) {
            return null;
        }
        try {
            return UUID.fromString(hubId.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_UUID_FORMAT);
        }
    }

    // 생략(null/빈 값)은 전체 조회, 정의되지 않은 값은 보정할 기본값이 없으므로 400으로 거절한다.
    private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
