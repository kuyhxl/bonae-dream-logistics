package com.bonae.logistics.message.presentation.docs;

import com.bonae.logistics.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * 서비스 간 내부 호출(/api/internal/**) 전용 공통 에러 응답.
 * 게이트웨이가 외부 인입을 차단하므로 인가(401/403)는 발생하지 않고, 검증 실패와 서버 오류만 남는다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = ApiErrorExamples.INVALID_INPUT_DESC,
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(
                                name = "INVALID_INPUT",
                                value = ApiErrorExamples.INVALID_INPUT_EXAMPLE))),
        @ApiResponse(
                responseCode = "500",
                description = ApiErrorExamples.INTERNAL_ERROR_DESC,
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(
                                name = "INTERNAL_ERROR",
                                value = ApiErrorExamples.INTERNAL_ERROR_EXAMPLE)))
})
public @interface InternalErrorResponses {
}
