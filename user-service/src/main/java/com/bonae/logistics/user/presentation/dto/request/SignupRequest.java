package com.bonae.logistics.user.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequest {
    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 4, max = 10, message = "아이디는 4~10자여야 합니다.")
    @Pattern(
            regexp = "^[a-z0-9]+$",
            message = "아이디는 소문자와 숫자만 사용할 수 있습니다."
    )
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 15, message = "비밀번호는 8~15자여야 합니다.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+])[A-Za-z\\d!@#$%^&*()_+]+$",
            message = "비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다."
    )
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "슬랙 ID는 필수입니다.")
    @Size(max = 100)
    private String slackId;

    @NotBlank(message = "소속 업체명 또는 허브명은 필수 입력입니다.")
    @Size(max = 100)
    private String affiliationName;
}
