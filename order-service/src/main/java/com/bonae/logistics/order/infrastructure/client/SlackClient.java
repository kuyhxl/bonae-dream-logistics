package com.bonae.logistics.order.infrastructure.client;

import com.bonae.logistics.order.infrastructure.client.dto.request.SlackMessageRequestDto;
import com.bonae.logistics.order.infrastructure.client.dto.response.SlackMessageResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "message-service")
public interface SlackClient {

    @PostMapping("/api/internal/slack-messages")
    SlackMessageResponseDto sendMessage(@RequestBody SlackMessageRequestDto request);
}
