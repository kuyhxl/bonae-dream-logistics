package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupResponse {
    private UUID userId; // 생성된 사용자 식별자
    private String username; // 로그인 ID
    private String name;
    private Status status; // 가입 상태(PENDING 고정)
    private LocalDateTime createdAt; // 신청 일시

    public SignupResponse(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.status = user.getStatus();
        this.createdAt = user.getCreatedAt();
    }
}
