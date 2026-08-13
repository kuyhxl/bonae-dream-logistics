package com.bonae.logistics.user.presentation.controller;

import com.bonae.logistics.common.response.ErrorResponse;
import com.bonae.logistics.user.application.service.AuthService;
import com.bonae.logistics.user.presentation.dto.request.LoginRequest;
import com.bonae.logistics.user.presentation.dto.request.LogoutRequest;
import com.bonae.logistics.user.presentation.dto.request.RefreshRequest;
import com.bonae.logistics.user.presentation.dto.request.SignupRequest;
import com.bonae.logistics.user.presentation.dto.response.LoginResponse;
import com.bonae.logistics.user.presentation.dto.response.SignupResponse;
import com.bonae.logistics.user.presentation.dto.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "회원가입 · 로그인 · 토큰 관리 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/signup")
    @SecurityRequirements // 토큰 없이 호출한다.
    @Operation(summary = "회원가입 신청",
            description = "가입을 신청한다. 생성 직후 상태는 PENDING이며, 마스터 관리자의 승인 이후에만 로그인할 수 있다.\n\n**접근 권한**: 누구나 (인증 불필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "가입 신청 완료"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패 (INVALID_INPUT). 항목별 사유는 fields에 담긴다.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 아이디 (USERNAME_DUPLICATED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SignupResponse> signup(@RequestBody @Valid SignupRequest signupRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(signupRequest));
    }

    @PostMapping("/auth/login")
    @SecurityRequirements // 토큰 없이 호출한다.
    @Operation(summary = "로그인",
            description = "아이디와 비밀번호로 인증하고 액세스 토큰과 리프레시 토큰을 발급한다. 리프레시 토큰은 서버에 저장되어 로그아웃 시 폐기된다.\n\n**접근 권한**: 누구나 (인증 불필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호가 일치하지 않음 (LOGIN_FAILED). 어느 쪽이 틀렸는지는 구분해서 알려주지 않는다.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "승인 대기 중이거나 거절된 계정 (USER_NOT_APPROVED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse> Login(@RequestBody @Valid LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "로그아웃",
            description = "저장된 리프레시 토큰을 폐기하고, Authorization 헤더로 함께 전달된 액세스 토큰을 블랙리스트에 등록해 남은 유효기간 동안 즉시 무효화한다.\n\n**접근 권한**: 로그인한 모든 사용자")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 완료"),
            @ApiResponse(responseCode = "401", description = "리프레시 토큰이 유효하지 않거나(INVALID_REFRESH_TOKEN), 만료되었거나(EXPIRED_REFRESH_TOKEN), 이미 폐기됨(REFRESH_TOKEN_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "토큰 저장소(Redis) 장애로 로그아웃을 확정할 수 없음 (SERVICE_UNAVAILABLE)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestBody @Valid LogoutRequest logoutRequest) {

        authService.logout(authorizationHeader, logoutRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/refresh")
    @Operation(summary = "토큰 재발급",
            description = "리프레시 토큰으로 액세스 토큰을 재발급한다. 리프레시 토큰도 함께 교체(RTR)되며, 이미 회전된 옛 토큰이 다시 들어오면 탈취로 간주해 저장된 토큰까지 폐기하고 재로그인을 유도한다.\n\n**접근 권한**: 로그인한 모든 사용자")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "리프레시 토큰이 유효하지 않거나 만료됨 (INVALID_REFRESH_TOKEN, EXPIRED_REFRESH_TOKEN, REFRESH_TOKEN_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "토큰 발급 이후 탈퇴한 사용자 (USER_NOT_FOUND)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "승인이 취소되었거나 거절된 계정 (USER_NOT_APPROVED)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TokenResponse> refresh(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestBody @Valid RefreshRequest refreshRequest) {
        return ResponseEntity.ok(authService.refresh(authorizationHeader, refreshRequest));
    }
}
