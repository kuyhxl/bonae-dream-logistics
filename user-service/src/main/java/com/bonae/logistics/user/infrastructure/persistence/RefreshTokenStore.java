package com.bonae.logistics.user.infrastructure.persistence;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public void save(String username, String refreshToken, long ttlMillis) {
        redisTemplate.opsForValue().set(KEY_PREFIX + username, refreshToken, Duration.ofMillis(ttlMillis));
    }

    public Optional<String> find(String username) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + username));
        } catch (DataAccessException e) { // redis 연결 실패
            log.error("Redis 조회 실패 - username={}", username, e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    public void delete(String username) {
        redisTemplate.delete(KEY_PREFIX + username);
    }
}
