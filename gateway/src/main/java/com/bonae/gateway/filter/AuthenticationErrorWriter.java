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
    private static final String UNAUTHORIZED_CODE = "UNAUTHORIZED";
    private static final String UNAUTHORIZED_MESSAGE = "인증이 필요합니다";
    private static final String SERVICE_UNAVAILABLE_CODE = "SERVICE_UNAVAILABLE";
    private static final String SERVICE_UNAVAILABLE_MESSAGE = "일시적으로 서비스를 사용할 수 없습니다. ";

    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public AuthenticationErrorWriter(ObjectMapper objectMapper, Tracer tracer) {
        this.objectMapper = objectMapper;
        this.tracer = tracer;
    }

    public Mono<Void> unauthorized(ServerWebExchange exchange) {
        return write(exchange, HttpStatus.UNAUTHORIZED, UNAUTHORIZED_CODE, UNAUTHORIZED_MESSAGE);
    }

    // 블랙리스트 조회 실패 등 인증을 확정할 수 없는 상황에 사용한다. 토큰 자체는 문제가 없으므로 503으로 알림
    public Mono<Void> serviceUnavailable(ServerWebExchange exchange) {
        return write(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                SERVICE_UNAVAILABLE_CODE, SERVICE_UNAVAILABLE_MESSAGE);
    }

    private Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("traceId", currentTraceId());

        byte[] bytes;

        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            log.error("에러 응답 직렬화 실패", e);
            bytes = ("{\"code\":\"" + code + "\"}").getBytes(StandardCharsets.UTF_8);
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