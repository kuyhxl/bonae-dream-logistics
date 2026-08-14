package com.bonae.logistics.delivery.infrastructure.client;

import com.bonae.logistics.delivery.infrastructure.client.dto.AiDispatchClientRequest;
import com.bonae.logistics.delivery.infrastructure.client.dto.AiDispatchClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "message-service")
public interface MessageClient {

    @PostMapping("/api/internal/ai-dispatch-logs")
    AiDispatchClientResponse createAiDispatch(@RequestBody AiDispatchClientRequest request);
}
