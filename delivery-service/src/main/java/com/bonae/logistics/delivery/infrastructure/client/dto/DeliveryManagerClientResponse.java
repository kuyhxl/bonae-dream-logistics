package com.bonae.logistics.delivery.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryManagerClientResponse {

    private UUID userId;
    private String userName;
    private String name;
    private String slackId;
    private UUID hubId;
}
