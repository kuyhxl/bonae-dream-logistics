package com.bonae.logistics.message.presentation.controller;

import com.bonae.logistics.message.application.service.AiDispatchService;
import com.bonae.logistics.message.presentation.dto.request.AiDispatchRequest;
import com.bonae.logistics.message.presentation.dto.response.AiDispatchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/* 배송 서비스 -> 메시지 서비스. 게이트웨이가 외부 인입을 차단 */
@RestController
@RequestMapping("/api/internal/ai-dispatch-logs")
@RequiredArgsConstructor
public class InternalAiDispatchController {

    private final AiDispatchService aiDispatchService;

    @PostMapping
    public ResponseEntity<AiDispatchResponse> create(@Valid @RequestBody AiDispatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AiDispatchResponse.from(aiDispatchService.calculateAndNotify(request.toCommand())));
    }
}