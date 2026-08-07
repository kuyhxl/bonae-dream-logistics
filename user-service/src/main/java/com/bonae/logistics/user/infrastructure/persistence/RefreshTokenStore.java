package com.bonae.logistics.user.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public void save(String username, String refreshToken, long ttlMillis) {
        redisTemplate.opsForValue().set(KEY_PREFIX + username, refreshToken, Duration.ofMillis(ttlMillis));
    }

    public Optional<String> find(String username) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + username));
    }

    public void delete(String username) {
        redisTemplate.delete(KEY_PREFIX + username);
    }
}
