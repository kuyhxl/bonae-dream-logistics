package com.bonae.logistics.common.exception;

import com.bonae.logistics.common.response.ErrorResponse;
import feign.FeignException;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    // PostgreSQL SQLState:unique_violation
    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

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

    // 요청 본문을 읽을 수 없음(깨진 JSON, 타입 불일치, 본문 누락)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        log.warn("[{}] 요청 본문을 읽을 수 없음: {}", errorCode.name(), e.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, errorCode.getMessage(), currentTraceId()));
    }

    // 필수 요청 헤더 누락
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException e) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        log.warn("[{}] 필수 요청 헤더 누락: header={}", errorCode.name(), e.getHeaderName());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, errorCode.getMessage(), currentTraceId()));
    }

    // 매핑되지 않은 경로 ->  없는 리소스이므로 404
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception e) {
        ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;
        log.warn("[{}] 매핑되지 않은 경로: {}", errorCode.name(), e.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, errorCode.getMessage(), currentTraceId()));
    }

    // 경로는 있으나 메서드가 다름
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        log.warn("[{}] 지원하지 않는 메서드: {}", errorCode.name(), e.getMethod());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, errorCode.getMessage(), currentTraceId()));
    }

    // DB 제약조건 위반. 각 서비스가 제약조건명으로 구체적인 원인을 구분해 변환하고, 분류 못한 경우만 여기로 온다.
    //  - unique 위반: 클라이언트가 이미 존재하는 값을 보낸 것이므로 409
    //  - 그 외(NOT NULL, 길이 초과 등): @Valid나 엔티티 매핑에서 걸렀어야 할 값이 DB까지 내려간 것임으로 500
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String sqlState = extractSqlState(e);

        if (UNIQUE_VIOLATION_SQL_STATE.equals(sqlState)) {
            ErrorCode errorCode = ErrorCode.DATA_CONFLICT;
            log.warn("[{}] 유니크 제약조건 위반: {}", errorCode.name(), e.getMostSpecificCause().getMessage());
            return ResponseEntity
                    .status(errorCode.getStatus())
                    .body(ErrorResponse.of(errorCode, errorCode.getMessage(), currentTraceId()));
        }

        // 재시도해도 통과할 수 없는 서버 결함이므로 알림에 걸리도록 error로
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        log.error("[{}] 데이터 제약조건 위반(sqlState={}): {}",
                errorCode.name(), sqlState, e.getMostSpecificCause().getMessage(), e);
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, errorCode.getMessage(), currentTraceId()));
    }

    // PostgreSQLDialect는 ConstraintViolationException의 ConstraintKind를 채우지 않아 항상 OTHER가 되므로 -> 원본 SQLException의 SQLState로 판단한다.
    private String extractSqlState(DataIntegrityViolationException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) {
                return sqlException.getSQLState();
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return null;
    }

    // 서비스 간 호출 실패. FeignErrorDecoder가 하위 서비스의 ErrorCode를 복원하지 못한 경우 (502)
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeign(FeignException e) {
        ErrorCode errorCode = ErrorCode.UPSTREAM_ERROR;
        log.error("[{}] 서비스 간 호출 실패: status={}", errorCode.name(), e.status(), e);
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