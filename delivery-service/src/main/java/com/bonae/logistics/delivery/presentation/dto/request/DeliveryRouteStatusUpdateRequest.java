package com.bonae.logistics.delivery.presentation.dto.request;

import com.bonae.logistics.delivery.domain.entity.RouteStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeliveryRouteStatusUpdateRequest {

    @NotNull(message = "경로 상태는 필수입니다.")
    private RouteStatus routeStatus;
}
