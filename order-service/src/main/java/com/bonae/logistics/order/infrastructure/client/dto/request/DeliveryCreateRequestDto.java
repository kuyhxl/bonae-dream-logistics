package com.bonae.logistics.order.infrastructure.client.dto.request;

import java.util.UUID;

//배송 생성 요청 Dto
public record DeliveryCreateRequestDto(
        UUID orderId,
        UUID supplierCompanyId,  //공급 업체
        UUID receiverCompanyId,
        String productInfo,
        String requestNote  // Order의 remarks와 매핑됨
) {
}
