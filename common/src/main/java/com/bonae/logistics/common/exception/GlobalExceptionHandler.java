package com.bonae.logistics.common.exception;

import com.bonae.logistics.common.response.ErrorResponse;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Tracer tracer;

    //비즈니스 규칙 위반
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[{}] {}", errorCode.name(), e.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, e.getMessage(), currentTraceId()));
    }

    // 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> fields = e.getBindingResult().getFieldErrors().stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .reason(error.getDefaultMessage())
                        .build())
                .toList();

        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        log.warn("[{}] {}", errorCode.name(), fields);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, errorCode.getMessage(), currentTraceId(), fields));
    }

    // 필수 요청 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(MissingServletRequestParameterException e) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        log.warn("[{}] 필수 요청 파라미터 누락: parameter={}", errorCode.name(), e.getParameterName());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, errorCode.getMessage(), currentTraceId()));
    }

    // 요청 파라미터 타입 변환 실패
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        log.warn("[{}] 요청 파라미터 타입 변환 실패: parameter={}", errorCode.name(), e.getName()
        );
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, errorCode.getMessage(), currentTraceId()));
    }

    // 그 외 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        log.error("[{}] 예상하지 못한 오류", errorCode.name(), e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, errorCode.getMessage(), currentTraceId()));
    }

    // Zipkin 추적 ID. 이 값으로 전 서비스 로그를 추적함
    private String currentTraceId() {
        Span span = tracer.currentSpan();
        return span == null ? null : span.context().traceId();
    }
}