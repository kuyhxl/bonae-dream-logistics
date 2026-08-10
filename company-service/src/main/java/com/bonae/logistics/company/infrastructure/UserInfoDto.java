package com.bonae.logistics.company.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

// user-service GET /api/internal/users/{username} 응답 중 이 서비스가 필요한 필드만 담는다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserInfoDto(UUID hubId, UUID companyId) {
}