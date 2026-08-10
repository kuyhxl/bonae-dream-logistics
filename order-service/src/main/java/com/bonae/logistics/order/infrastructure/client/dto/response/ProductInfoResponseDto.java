package com.bonae.logistics.order.infrastructure.client.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductInfoResponseDto(
        UUID productId,
        String name,
        UUID companyId,      // supplierCompanyId
        BigDecimal price,
        boolean isDeleted
) {
}
