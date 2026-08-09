package com.bonae.logistics.hub.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "허브 생성 요청")
public class HubCreateRequest {

    @NotBlank(message = "허브명은 필수입니다.")
    @Size(max = 100, message = "허브명은 100자 이하로 입력해주세요.")
    @Schema(description = "허브명", example = "서울특별시 센터")
    private String name;

    @NotBlank(message = "허브 주소는 필수입니다.")
    @Size(max = 255, message = "허브 주소는 255자 이하로 입력해주세요.")
    @Schema(description = "허브 주소", example = "서울특별시 송파구 송파대로 55")
    private String address;

    @NotNull(message = "위도는 필수입니다.")
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
    @Schema(description = "위도", example = "37.4742027808565")
    private Double latitude;

    @NotNull(message = "경도는 필수입니다.")
    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
    @Schema(description = "경도", example = "127.123621185562")
    private Double longitude;

    public String getName() {
        return name == null ? null : name.strip();
    }

    // 앞뒤 공백 제거와 연속 공백 축소를 함께 적용한다.
    public String getAddress() {
        return address == null ? null : address.strip().replaceAll("\\s+", " ");
    }

}
