package com.bonae.logistics.company.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CompanyErrorCode {
    //TODO : 공통 에러 임시로 저장함. 추후에 삭제 예정
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN,"접근 권한이 없습니다."),

    INVALID_COMPANY_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 업체 유형입니다."),
    COMPANY_DUPLICATED(HttpStatus.CONFLICT, "이미 동일한 업체명과 주소로 등록된 업체가 존재합니다."),

    HUB_NOT_FOUND(HttpStatus.NOT_FOUND,"허브를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
