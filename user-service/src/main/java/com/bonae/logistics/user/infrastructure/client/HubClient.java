package com.bonae.logistics.user.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

// 존재 여부만 확인하면 되므로 응답 본문은 받지 않는다.
// 없거나 삭제된 허브면 hub-service가 HUB_NOT_FOUND(404)를 주고,
// common의 FeignErrorDecoder가 이를 BusinessException(HUB_NOT_FOUND)로 복원해 그대로 전파된다.
@FeignClient(name = "hub-service")
public interface HubClient {

    @GetMapping("/api/internal/hubs/{hubId}")
    void validateHubExists(@PathVariable("hubId") UUID hubId);
}