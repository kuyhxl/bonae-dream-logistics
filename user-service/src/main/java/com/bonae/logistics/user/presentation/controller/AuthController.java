package com.bonae.logistics.user.presentation.controller;

import com.bonae.logistics.user.application.service.AuthService;
import com.bonae.logistics.user.presentation.dto.request.LoginRequest;
import com.bonae.logistics.user.presentation.dto.request.LogoutRequest;
import com.bonae.logistics.user.presentation.dto.request.SignupRequest;
import com.bonae.logistics.user.presentation.dto.response.LoginResponse;
import com.bonae.logistics.user.presentation.dto.response.SignupResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody @Valid SignupRequest signupRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(signupRequest));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> Login(@RequestBody @Valid LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestBody @Valid LogoutRequest logoutRequest) {

        authService.logout(authorizationHeader, logoutRequest);
        return ResponseEntity.noContent().build();
    }
}
