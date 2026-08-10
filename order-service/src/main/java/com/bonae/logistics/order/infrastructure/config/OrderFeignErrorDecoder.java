package com.bonae.logistics.order.infrastructure.config;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.warn("Feign 호출 실패: method={}, status={}", methodKey, response.status());

        return switch (response.status()) {
            case 404 -> resolveNotFound(methodKey);
            case 409 -> new BusinessException(ErrorCode.STOCK_SHORTAGE);
            case 500, 502, 503 -> new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
            default -> defaultDecoder.decode(methodKey, response);
        };
    }

    // methodKey-어떤 Client의 어떤 메서드에서 실패했는지 구분하는 용도
    private Exception resolveNotFound(String methodKey) {
        if (methodKey.contains("InventoryClient")) {
            return new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (methodKey.contains("DeliveryClient")) {
            return new BusinessException(ErrorCode.COMPANY_NOT_FOUND);
        }
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
}