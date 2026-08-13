package com.bonae.logistics.message.presentation.controller;

import com.bonae.logistics.message.application.service.AiDispatchService;
import com.bonae.logistics.message.presentation.docs.InternalErrorResponses;
import com.bonae.logistics.message.presentation.dto.request.AiDispatchRequest;
import com.bonae.logistics.message.presentation.dto.response.AiDispatchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/* 배송 서비스 -> 메시지 서비스. 게이트웨이가 외부 인입을 차단 */
@RestController
@RequestMapping("/api/internal/ai-dispatch-logs")
@RequiredArgsConstructor
@Tag(name = "Internal AI Dispatch", description = """
        AI 발송 시한 산출 및 담당자 알림. 서비스 간 내부 호출 전용 API.

        **접근 권한** — 내부 호출 전용. 게이트웨이가 `/api/internal/**` 외부 인입을 차단하므로
        `X-User-Role` 기반 인가(401/403)를 적용하지 않는다.""")
public class InternalAiDispatchController {

    private final AiDispatchService aiDispatchService;

    @PostMapping
    @Operation(
            summary = "(내부) AI 발송 시한 산출 및 슬랙 알림",
            description = """
                    주문·경로 정보로 AI에게 최종 발송 시한을 계산시키고, 결과를 발송 허브 담당자에게 슬랙으로 알린다.

                    **접근 권한** — 내부 호출 전용

                    AI 호출 실패나 조건에 맞지 않는 응답은 산술 폴백으로 대체하고 에러 로그만 남긴다.
                    슬랙 발송 실패도 마찬가지로 예외를 전파하지 않으므로,
                    두 경우 모두 **201**로 응답한다(배송 생성을 막지 않기 위한 설계).""")
    @InternalErrorResponses
    @ApiResponse(responseCode = "201", description = "시한 산출 및 알림 처리 완료 (AI·슬랙 실패 시 폴백 결과 포함)")
    public ResponseEntity<AiDispatchResponse> create(@Valid @RequestBody AiDispatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AiDispatchResponse.from(aiDispatchService.calculateAndNotify(request.toCommand())));
    }
}
