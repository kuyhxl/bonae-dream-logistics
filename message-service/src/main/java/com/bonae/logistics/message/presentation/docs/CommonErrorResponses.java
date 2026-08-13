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
 * 인증이 필요한 모든 엔드포인트가 공통으로 낼 수 있는 에러 응답.
 * 엔드포인트마다 반복해 쓰지 않도록 합성 어노테이션으로 묶었다.
 * (springdoc은 메타 어노테이션의 @ApiResponse까지 병합해 읽는다.)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = ApiErrorExamples.INVALID_INPUT_DESC
                        + " / " + ApiErrorExamples.INVALID_SORT_FIELD_DESC + " (페이징 엔드포인트)",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = {
                                @ExampleObject(
                                        name = "INVALID_INPUT",
                                        value = ApiErrorExamples.INVALID_INPUT_EXAMPLE),
                                @ExampleObject(
                                        name = "INVALID_SORT_FIELD",
                                        value = ApiErrorExamples.INVALID_SORT_FIELD_EXAMPLE)})),
        @ApiResponse(
                responseCode = "401",
                description = ApiErrorExamples.UNAUTHORIZED_DESC,
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(
                                name = "UNAUTHORIZED",
                                value = ApiErrorExamples.UNAUTHORIZED_EXAMPLE))),
        @ApiResponse(
                responseCode = "403",
                description = ApiErrorExamples.FORBIDDEN_DESC,
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponse.class),
                        examples = @ExampleObject(
                                name = "FORBIDDEN",
                                value = ApiErrorExamples.FORBIDDEN_EXAMPLE))),
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
public @interface CommonErrorResponses {
}
