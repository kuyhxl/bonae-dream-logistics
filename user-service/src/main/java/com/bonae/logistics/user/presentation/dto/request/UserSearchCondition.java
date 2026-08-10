package com.bonae.logistics.user.presentation.dto.request;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.user.domain.entity.Role;
import com.bonae.logistics.user.domain.entity.Status;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.UUID;

// 잘못된 값을 그대로 바인딩하면 BindException(-> 500)이 되므로 문자열로 받아 직접 변환한다.
@Getter
@Setter
public class UserSearchCondition {

    private String keyword;
    private String role;
    private String status;
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