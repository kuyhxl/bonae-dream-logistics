package com.bonae.logistics.order.infrastructure.client.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

//배송 생성 요청 Dto
public record DeliveryCreateRequestDto(
        UUID orderId,
        UUID supplierCompanyId,
        UUID receiverCompanyId,
        String receiverUsername,
        String productName,
        Integer quantity,
        LocalDateTime dueDate,
        String requestNote  // Order의 remarks와 매핑됨
) {}