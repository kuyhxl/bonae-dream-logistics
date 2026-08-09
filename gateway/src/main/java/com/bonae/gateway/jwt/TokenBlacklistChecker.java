package com.bonae.gateway.jwt;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class TokenBlacklistChecker {

    private static final String KEY_PREFIX = "blacklist:";

    private final ReactiveStringRedisTemplate redisTemplate;

    public TokenBlacklistChecker(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 조회에 실패하면 예외를 그대로 전파함
    public Mono<Boolean> isBlacklisted(String jti) {
        return redisTemplate.hasKey(KEY_PREFIX + jti);
    }
}
