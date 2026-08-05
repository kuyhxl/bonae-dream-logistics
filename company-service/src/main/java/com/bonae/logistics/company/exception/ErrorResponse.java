//package com.bonae.logistics.company.exception;
//
//import lombok.Builder;
//import lombok.Getter;
//
//@Getter
//@Builder
//public class ErrorResponse {
//
//    private String code;
//    private String message;
//
//    public static ErrorResponse of(CompanyErrorCode errorCode) {
//        return ErrorResponse.builder()
//                .code(errorCode.name())
//                .message(errorCode.getMessage())
//                .build();
//    }
//
//    public static ErrorResponse of(CompanyErrorCode errorCode, String message) {
//        return ErrorResponse.builder()
//                .code(errorCode.name())
//                .message(message)
//                .build();
//    }
//}
