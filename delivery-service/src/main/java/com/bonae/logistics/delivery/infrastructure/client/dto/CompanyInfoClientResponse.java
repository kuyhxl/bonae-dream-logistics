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
public class CompanyInfoClientResponse {

    private UUID companyId;
    private String name;
    private String type;
    private UUID hubId;
    private String address;
    private boolean isDeleted;
}
