package com.bonae.logistics.hub.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "허브 수정 요청 (전달된 필드만 수정)")
public class HubUpdateRequest {

    @Size(max = 100, message = "허브명은 100자 이하로 입력해주세요.")
    @Schema(description = "허브명", example = "서울특별시 북부 센터")
    private String name;

    @Size(max = 255, message = "허브 주소는 255자 이하로 입력해주세요.")
    @Schema(description = "허브 주소", example = "서울특별시 노원구 동일로 1000")
    private String address;

    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
    @Schema(description = "위도")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
    @Schema(description = "경도")
    private Double longitude;

    public String getName() {
        return name == null ? null : name.strip();
    }

    public String getAddress() {
        return address == null ? null : address.strip().replaceAll("\\s+", " ");
    }

    // 수정할 필드가 하나도 없는 빈 요청인지 확인
    public boolean isEmpty() {
        return getName() == null && getAddress() == null && latitude == null && longitude == null;
    }
}