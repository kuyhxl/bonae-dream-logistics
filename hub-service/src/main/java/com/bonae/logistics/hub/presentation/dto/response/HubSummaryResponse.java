package com.bonae.logistics.hub.presentation.dto.response;

import com.bonae.logistics.hub.domain.entity.Hub;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class HubSummaryResponse {

    private UUID hubId;
    private String hubName;

    public static HubSummaryResponse from(Hub hub) {
        return new HubSummaryResponse(
                hub.getId(),
                hub.getName()
        );
    }
}