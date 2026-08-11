package com.bonae.logistics.user.presentation.dto.request;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.user.domain.entity.Status;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.Locale;

// UserSearchCondition과 같은 이유로(잘못된 값 바인딩 시 500 방지) 문자열로 받아 직접 변환한다.
@Getter
@Setter
public class SignupRequestSearchCondition {

    private String keyword;
    private String status;

    public String getKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    // 가입 "요청" 목록의 기본 관심사는 아직 처리되지 않은 건이므로 생략 시 PENDING으로 본다.
    // 승인·거절 이력을 보려면 status를 명시적으로 넘긴다.
    public Status toStatus() {
        if (!StringUtils.hasText(status)) {
            return Status.PENDING;
        }
        try {
            return Status.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}