package com.bonae.logistics.common.response;

import com.bonae.logistics.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String code;
    private final String message;
    private final String traceId;
    private final List<FieldError> fields;   // 검증 실패 시 포함

    public static ErrorResponse of(ErrorCode errorCode, String message, String traceId) {
        return ErrorResponse.builder()
                .code(errorCode.name())
                .message(message)
                .traceId(traceId)
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, String traceId,
                                   List<FieldError> fields) {
        return ErrorResponse.builder()
                .code(errorCode.name())
                .message(message)
                .traceId(traceId)
                .fields(fields)
                .build();
    }

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String reason;
    }
}