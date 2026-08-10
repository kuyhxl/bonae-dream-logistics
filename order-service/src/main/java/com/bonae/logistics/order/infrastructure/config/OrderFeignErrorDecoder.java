package com.bonae.logistics.order.infrastructure.config;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class OrderFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.warn("Feign 호출 실패: method={}, status={}", methodKey, response.status());

        try (InputStream body = response.body().asInputStream()) {
            ErrorResponse errorResponse = objectMapper.readValue(body, ErrorResponse.class);
            ErrorCode errorCode = resolveErrorCode(errorResponse.getCode(), response.status());
            return new BusinessException(errorCode, errorResponse.getMessage());
        } catch (IOException e) {
            log.error("Feign 에러 응답 파싱 실패", e);
            return fallback(response.status());
        }
    }

    private ErrorCode resolveErrorCode(String code, int status) {
        try {
            return ErrorCode.valueOf(code);
        } catch (IllegalArgumentException e) {
            return fallback(status) instanceof BusinessException be ? be.getErrorCode() : ErrorCode.INTERNAL_ERROR;
        }
    }

    private Exception fallback(int status) {
        return switch (status) {
            case 404 -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            case 409 -> new BusinessException(ErrorCode.STOCK_SHORTAGE);
            case 500, 502, 503 -> new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
            default -> new BusinessException(ErrorCode.INTERNAL_ERROR);
        };
    }
}