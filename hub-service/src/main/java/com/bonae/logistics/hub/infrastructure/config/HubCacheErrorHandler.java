package com.bonae.logistics.hub.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * 캐시 접근 실패를 삼켜 서비스가 계속 동작하게 한다.
 * 단, 조회/저장 실패와 무효화 실패는 잃는 것이 다르므로 로그 등급을 구분한다.
 * - 조회/저장 실패: 원본(DB)에서 정답을 그대로 얻으므로 성능만 저하된다.
 * - 무효화 실패: 변경 전 데이터가 TTL 만료까지 남아 stale 응답이 나갈 수 있다.
 */
@Slf4j
public class HubCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("[Redis] 캐시 조회 실패, DB 조회로 대체 cache={} key={}", cache.getName(), key, exception);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("[Redis] 캐시 저장 실패, 다음 요청도 DB 조회 cache={} key={}", cache.getName(), key, exception);
    }

    // RedisCacheManager가 transactionAware이므로 무효화는 DB 커밋 이후에 실행된다.
    // 즉 여기서 실패하면 DB만 변경되고 캐시에는 변경 전 값이 남는다.
    // 노출 창은 RedisCacheConfig에 설정한 캐시별 TTL로 제한되지만, 정합성을 잃는
    // 구간이므로 조회/저장 실패와 달리 error로 남겨 알림 대상이 되게 한다.
    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.error("[Redis] 캐시 무효화 실패, TTL 만료까지 stale 응답 가능 cache={} key={}",
                cache.getName(), key, exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.error("[Redis] 캐시 전체 삭제 실패, TTL 만료까지 stale 응답 가능 cache={}",
                cache.getName(), exception);
    }
}