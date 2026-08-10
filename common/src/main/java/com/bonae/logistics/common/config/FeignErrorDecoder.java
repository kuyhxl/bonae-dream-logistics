package com.bonae.logistics.common.config;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

@Slf4j
@RequiredArgsConstructor
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    @Override
    public Exception decode(String methodKey, Response response) {
        ErrorResponse body = readBody(response);

        if (body == null || body.getCode() == null) {
            log.warn("Feign 호출 실패(본문 해석 불가): method={}, status={}", methodKey, response.status());
            return new BusinessException(fallback(response.status()));
        }

        ErrorCode errorCode = toErrorCode(body.getCode());
        if (errorCode == null) {
            // 상대 서비스가 더 최신 ErrorCode를 쓸 때 등
            log.warn("Feign 호출 실패(알 수 없는 코드): method={}, status={}, code={}",
                    methodKey, response.status(), body.getCode());
            return new BusinessException(fallback(response.status()));
        }

        log.warn("Feign 호출 실패: method={}, status={}, code={}", methodKey, response.status(), errorCode.name());
        return new BusinessException(errorCode, body.getMessage());
    }

    private ErrorResponse readBody(Response response) {
        if (response.body() == null) {
            return null;
        }
        try (InputStream in = response.body().asInputStream()) {
            return objectMapper.readValue(in, ErrorResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    private ErrorCode toErrorCode(String code) {
        try {
            return ErrorCode.valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // 본문 해석을 못했을 때 HTTP 상태로만 판단
    private ErrorCode fallback(int status) {
        return switch (status) {
            case 400 -> ErrorCode.INVALID_INPUT;
            case 401 -> ErrorCode.UNAUTHORIZED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
            case 503 -> ErrorCode.SERVICE_UNAVAILABLE;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}
