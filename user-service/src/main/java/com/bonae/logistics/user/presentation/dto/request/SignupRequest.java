package com.bonae.logistics.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "회원가입 신청 요청")
public class SignupRequest {
    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 4, max = 10, message = "아이디는 4~10자여야 합니다.")
    @Pattern(
            regexp = "^[a-z0-9]+$",
            message = "아이디는 소문자와 숫자만 사용할 수 있습니다."
    )
    @Schema(description = "아이디. 소문자와 숫자만 사용하며 4~10자.", example = "hyerim01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 15, message = "비밀번호는 8~15자여야 합니다.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+])[A-Za-z\\d!@#$%^&*()_+]+$",
            message = "비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다."
    )
    @Schema(description = "비밀번호. 대문자·소문자·숫자·특수문자를 모두 포함한 8~15자.", example = "Password1!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 100)
    @Schema(description = "이름", example = "김혜림", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "슬랙 ID는 필수입니다.")
    @Size(max = 100)
    @Schema(description = "슬랙 ID. 배송 알림 발송에 사용한다.", example = "U01ABCDEF", requiredMode = Schema.RequiredMode.REQUIRED)
    private String slackId;

    @NotBlank(message = "소속 업체명 또는 허브명은 필수 입력입니다.")
    @Size(max = 100)
    @Schema(description = "소속 업체명 또는 허브명. 신청자가 자유롭게 입력하며, 승인 시 관리자가 실제 허브/업체와 연결한다.", example = "서울특별시 센터", requiredMode = Schema.RequiredMode.REQUIRED)
    private String affiliationName;
}
