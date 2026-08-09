package com.bonae.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "auth")
public record AuthPathProperties(List<String> permitAll) {

    public AuthPathProperties{
        permitAll = permitAll == null ? List.of() : List.copyOf(permitAll);
    }
}
