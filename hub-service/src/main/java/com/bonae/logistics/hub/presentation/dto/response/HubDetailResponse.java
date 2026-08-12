package com.bonae.logistics.hub.presentation.dto.response;

import com.bonae.logistics.hub.domain.entity.Hub;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@Jacksonized
@Schema(description = "허브 상세 응답")
public class HubDetailResponse {

    @Schema(description = "허브 ID")
    private UUID hubId;

    @Schema(description = "허브명")
    private String name;

    @Schema(description = "허브 주소")
    private String address;

    @Schema(description = "위도")
    private Double latitude;

    @Schema(description = "경도")
    private Double longitude;

    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    @Schema(description = "생성자")
    private String createdBy;

    @Schema(description = "수정 일시")
    private LocalDateTime updatedAt;

    @Schema(description = "수정자")
    private String updatedBy;


    public static HubDetailResponse from(Hub hub) {
        return HubDetailResponse.builder()
                .hubId(hub.getId())
                .name(hub.getName())
                .address(hub.getAddress())
                .latitude(hub.getLatitude())
                .longitude(hub.getLongitude())
                .createdAt(hub.getCreatedAt())
                .createdBy(hub.getCreatedBy())
                .updatedAt(hub.getUpdatedAt())
                .updatedBy(hub.getUpdatedBy())
                .build();
    }
}
