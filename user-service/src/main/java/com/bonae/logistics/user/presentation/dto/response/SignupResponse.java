package com.bonae.logistics.user.presentation.dto.response;

import com.bonae.logistics.user.domain.entity.Status;
import com.bonae.logistics.user.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "회원가입 신청 응답")
public class SignupResponse {

    @Schema(description = "생성된 사용자 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
    private UUID userId; // 생성된 사용자 식별자

    @Schema(description = "아이디", example = "hyerim01")
    private String username; // 로그인 ID

    @Schema(description = "이름", example = "김혜림")
    private String name;

    @Schema(description = "가입 상태. 신청 직후에는 항상 PENDING이다.", example = "PENDING")
    private Status status; // 가입 상태(PENDING 고정)

    @Schema(description = "신청 일시", example = "2026-08-13T10:15:30")
    private LocalDateTime createdAt; // 신청 일시

    public SignupResponse(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.status = user.getStatus();
        this.createdAt = user.getCreatedAt();
    }
}
