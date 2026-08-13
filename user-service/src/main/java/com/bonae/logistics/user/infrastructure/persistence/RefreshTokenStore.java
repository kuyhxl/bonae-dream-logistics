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

    /*
     * 리프레시 토큰은 Redis에만 보관하므로, 저장이 실패하면 재발급이 불가능한 상태가 된다.
     * 로그인을 성공으로 응답할 수 없어 503으로 알린다. (조회 실패와 같은 처리)
     */
    public void save(String username, String refreshToken, long ttlMillis) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + username, refreshToken, Duration.ofMillis(ttlMillis));
        } catch (DataAccessException e) { // redis 연결 실패
            log.error("Redis 저장 실패 - username={}", username, e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    public Optional<String> find(String username) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + username));
        } catch (DataAccessException e) { // redis 연결 실패
            log.error("Redis 조회 실패 - username={}", username, e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    /*
     * 삭제 실패는 로그아웃을 막지 않는다. 액세스 토큰은 블랙리스트로 이미 차단되고,
     * 남은 리프레시 토큰도 TTL이 지나면 사라지기 때문이다. (TokenBlacklistStore와 같은 판단)
     */
    public void delete(String username) {
        try {
            redisTemplate.delete(KEY_PREFIX + username);
        } catch (DataAccessException e) {
            log.error("Redis 삭제 실패 - username={}", username, e);
        }
    }
}
