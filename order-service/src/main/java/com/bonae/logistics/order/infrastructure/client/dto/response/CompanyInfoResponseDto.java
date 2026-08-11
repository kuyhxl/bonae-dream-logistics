package com.bonae.logistics.order.infrastructure.client.dto.response;

import java.util.UUID;

public record CompanyInfoResponseDto(
        UUID companyId,
        String name,
        String type,
        UUID hubId,
        String address
) {
}
