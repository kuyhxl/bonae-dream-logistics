package com.bonae.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class AuthenticationErrorWriter {

    // common 에러코드와 동일한 값
    private static final String ERROR_CODE = "UNAUTHORIZED";
    private static final String ERROR_MESSAGE = "인증이 필요합니다";

    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public AuthenticationErrorWriter(ObjectMapper objectMapper, Tracer tracer) {
        this.objectMapper = objectMapper;
        this.tracer = tracer;
    }

    public Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ERROR_CODE);
        body.put("message", ERROR_MESSAGE);
        body.put("traceId", currentTraceId());

        byte[] bytes;

        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            log.error("인증 실패 응답 직렬화 실패", e);
            bytes = ("{\"code\":\"" + ERROR_CODE + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    // Zipkin 추적 ID
    private String currentTraceId() {
        Span span = tracer.currentSpan();
        return span == null ? null : span.context().traceId();
    }
}