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
public class UserInfoClientResponse {

    private UUID id;
    private String username;
    private String name;
    private String slackId;
    private String role;
    private UUID hubId;
    private UUID companyId;
}
