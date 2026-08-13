package com.bonae.logistics.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

// 관리자의 처리 구분. 도메인 Status(PENDING 포함)와 달리 요청으로 올 수 있는 값만 정의한다.
@Schema(description = "가입 요청 처리 구분. 도메인 상태와 달리 요청으로 보낼 수 있는 값만 정의한다.")
public enum ApprovalStatus {
    APPROVED, REJECTED
}
