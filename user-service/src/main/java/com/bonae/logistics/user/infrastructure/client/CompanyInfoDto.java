package com.bonae.logistics.user.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

// company-service GET /api/internal/companies/{companyId} 응답 중 필요한 필드만 담는다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompanyInfoDto(UUID companyId, UUID hubId) {
}
