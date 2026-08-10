package com.bonae.logistics.common.response;

import com.bonae.logistics.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

// Feign이 하위 서비스의 에러 응답을 읽어야 하므로 역직렬화를 지원해줌
@Getter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
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
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FieldError {
        private final String field;
        private final String reason;
    }
}