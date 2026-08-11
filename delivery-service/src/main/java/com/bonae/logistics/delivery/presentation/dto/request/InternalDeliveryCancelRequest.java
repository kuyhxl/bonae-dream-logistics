package com.bonae.logistics.delivery.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class InternalDeliveryCancelRequest {

    @NotNull
    private UUID orderId;
}
