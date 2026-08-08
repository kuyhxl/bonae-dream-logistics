package com.bonae.logistics.user.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/*
 * 로그아웃된 accessToken의 jti를 만료 시각까지 보관한다.
 * 게이트웨이가 요청마다 이 키의 유무로 토큰을 거른다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistStore {

    private static final String KEY_PREFIX = "blacklist:";
    private static final String VALUE = "logout";

    private final StringRedisTemplate redisTemplate;

    public void add(String jti, long ttlMillis) {
        if (ttlMillis <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + jti, VALUE, Duration.ofMillis(ttlMillis));
        } catch (DataAccessException e) {
            // 로그아웃 성공
            log.error("블랙리스트 등록 실패 - jti={}", jti, e);
        }
    }
}
