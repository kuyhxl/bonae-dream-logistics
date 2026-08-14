package com.bonae.logistics.delivery.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiDispatchClientRequest {

    private UUID orderId;
    private String orderNo;
    private String ordererName;
    private String ordererSlackId;
    private String productName;
    private Integer quantity;
    private LocalDateTime dueDate;
    private String requestNote;
    private String originHubName;
    private List<String> waypointHubNames;
    private String destinationAddress;
    private Integer totalDurationMin;
    private String managerSlackId;
}
