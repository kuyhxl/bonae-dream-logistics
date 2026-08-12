package com.bonae.logistics.message.infrastructure.client;

import com.bonae.logistics.message.infrastructure.client.dto.UserInfoClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/internal/users/{username}")
    UserInfoClientResponse getUserInfo(@PathVariable("username") String username);
}
