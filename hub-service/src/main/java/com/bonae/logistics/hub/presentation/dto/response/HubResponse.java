package com.bonae.logistics.hub.presentation.dto.response;

import com.bonae.logistics.hub.domain.entity.Hub;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class HubResponse {
    private UUID hubId;

    public static HubResponse from(Hub hub) {
        return new HubResponse(hub.getId());
    }
}
