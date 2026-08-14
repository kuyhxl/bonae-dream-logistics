package com.bonae.logistics.delivery.infrastructure.client;

import com.bonae.logistics.delivery.domain.entity.ManagerType;
import com.bonae.logistics.delivery.infrastructure.client.dto.DeliveryManagerClientResponse;
import com.bonae.logistics.delivery.infrastructure.client.dto.UserInfoClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/internal/users/{username}")
    UserInfoClientResponse getUserInfo(@PathVariable("username") String username);

    @GetMapping("/api/internal/users/delivery-managers")
    List<DeliveryManagerClientResponse> getDeliveryManagers(
            @RequestParam(value = "hubId", required = false) UUID hubId,
            @RequestParam("type") ManagerType type
    );
}
