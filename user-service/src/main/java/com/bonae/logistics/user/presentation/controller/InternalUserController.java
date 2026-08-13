package com.bonae.logistics.user.presentation.controller;

import com.bonae.logistics.common.response.ErrorResponse;
import com.bonae.logistics.user.application.service.UserService;
import com.bonae.logistics.user.domain.entity.DeliveryManagerType;
import com.bonae.logistics.user.presentation.dto.response.DeliveryManagerResponse;
import com.bonae.logistics.user.presentation.dto.response.UserInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// 서비스 간 내부 호출 전용. springdoc.paths-to-exclude 설정으로 공개 문서에서는 제외되므로
// 아래 어노테이션은 Swagger UI에 노출되지 않고 코드상의 명세 역할만 한다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/users")
@Tag(name = "Internal User", description = "서비스 간 내부 호출 전용 사용자 API")
public class InternalUserController {

    private final UserService userService;

    @GetMapping("delivery-managers")
    @Operation(summary = "(내부) 배송 담당자 목록 조회",
            description = "배송 배정 후보를 조회한다. 조건에 맞는 담당자가 없어도 예외 없이 빈 목록을 반환하며, 배정 가능 여부 판단은 호출 측 책임이다."
                    + "\n\n**접근 권한**: 게이트웨이를 거치지 않는 서비스 간 내부 호출 전용")
    @ApiResponse(responseCode = "200", description = "조회 성공. 조건에 맞는 담당자가 없으면 빈 목록")
    public ResponseEntity<List<DeliveryManagerResponse>> getDeliveryManagers(
            @Parameter(description = "소속 허브 ID. 생략하면 허브로 필터링하지 않는다.", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
            @RequestParam(required = false) UUID hubId,
            @Parameter(description = "담당자 유형. 생략하면 유형으로 필터링하지 않는다.")
            @RequestParam(required = false) DeliveryManagerType type
    ) {
        return ResponseEntity.ok(userService.getDeliveryManagers(hubId, type));
    }

    @GetMapping("{username}")
    @Operation(summary = "(내부) 사용자 정보 조회",
            description = "아이디로 사용자 정보를 조회한다. delivery-service의 담당자 배정 알림, message-service의 수신자 슬랙 ID 조회에 사용한다."
                    + "\n\n논리 삭제되었거나 승인되지 않은 사용자는 호출 측에서 동일하게 처리하므로 구분 없이 404로 응답한다."
                    + "\n\n**접근 권한**: 게이트웨이를 거치지 않는 서비스 간 내부 호출 전용")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자가 없거나, 논리 삭제 또는 미승인 상태 (USER_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserInfoResponse> getUserInfo(
            @Parameter(description = "사용자 아이디", example = "hyerim01") @PathVariable String username) {
        return ResponseEntity.ok(userService.getUserInfo(username));
    }
}
