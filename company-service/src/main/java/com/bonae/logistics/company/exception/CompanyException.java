package com.bonae.logistics.company.exception;

import lombok.Getter;

@Getter
//TODO : 공통 예외 클래스 생성 전 임시로 사용. 추후에 삭제 예정
public class CompanyException extends RuntimeException {

    private final CompanyErrorCode errorCode;

    public CompanyException(CompanyErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
