package com.bonae.logistics.common.config;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeignErrorDecoder 에러 응답 변환")
class FeignErrorDecoderTest {

    private final FeignErrorDecoder decoder = new FeignErrorDecoder(new ObjectMapper());

    private Response response(int status, String body) {
        Request request = Request.create(
                Request.HttpMethod.POST, "/api/internal/inventories/deduct",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());

        Response.Builder builder = Response.builder().request(request).status(status);
        if (body != null) {
            builder.body(body, StandardCharsets.UTF_8);
        }
        return builder.build();
    }

    private ErrorCode decodedCodeOf(Response response) {
        Exception e = decoder.decode("InventoryClient#deduct(Dto)", response);
        assertThat(e).isInstanceOf(BusinessException.class);
        return ((BusinessException) e).getErrorCode();
    }

    @Test
    @DisplayName("응답 본문의 code를 그대로 도메인 예외로 되돌린다")
    void restoresErrorCodeFromBody() {
        String body = """
                {"code":"STOCK_SHORTAGE","message":"재고가 부족합니다. ","traceId":"abc123"}
                """;

        assertThat(decodedCodeOf(response(409, body))).isEqualTo(ErrorCode.STOCK_SHORTAGE);
    }

    @Test
    @DisplayName("메시지도 함께 전달")
    void keepsMessage() {
        String body = """
                {"code":"PRODUCT_NOT_FOUND","message":"상품을 찾을 수 없습니다. ","traceId":null}
                """;

        Exception e = decoder.decode("InventoryClient#deduct(Dto)", response(404, body));

        assertThat(e).hasMessage("상품을 찾을 수 없습니다. ");
    }

    @Test
    @DisplayName("fields가 포함된 검증 실패 응답도 해석한다")
    void handlesValidationResponse() {
        String body = """
                {"code":"INVALID_INPUT","message":"입력값이 올바르지 않습니다.","traceId":"t",
                 "fields":[{"field":"quantity","reason":"1 이상이어야 합니다."}]}
                """;

        assertThat(decodedCodeOf(response(400, body))).isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("알 수 없는 code면 상태코드로 판단한다")
    void unknownCodeFallsBack() {
        // 상대 서비스가 이쪽에 없는 ErrorCode를 쓰는 경우
        String body = """
                {"code":"SOME_FUTURE_CODE","message":"미래 오류","traceId":"t"}
                """;

        assertThat(decodedCodeOf(response(404, body))).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("본문이 우리 형식이 아니면 상태코드로 판단한다")
    void unparsableBodyFallsBack() {
        assertThat(decodedCodeOf(response(503, "<html>Service Unavailable</html>")))
                .isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("본문이 없어도 예외를 만든다")
    void emptyBodyFallsBack() {
        assertThat(decodedCodeOf(response(500, null))).isEqualTo(ErrorCode.INTERNAL_ERROR);
    }

    @Test
    @DisplayName("인증·인가 실패는 상태코드에 맞는 기본값으로 변환한다")
    void authFailuresFallBack() {
        assertThat(decodedCodeOf(response(401, ""))).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(decodedCodeOf(response(403, ""))).isEqualTo(ErrorCode.FORBIDDEN);
    }
}
