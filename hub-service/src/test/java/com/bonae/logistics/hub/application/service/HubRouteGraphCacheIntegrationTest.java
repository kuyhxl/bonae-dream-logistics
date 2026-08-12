package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.hub.domain.entity.Hub;
import com.bonae.logistics.hub.domain.entity.HubRoute;
import com.bonae.logistics.hub.domain.repository.HubRepository;
import com.bonae.logistics.hub.domain.repository.HubRouteRepository;
import com.bonae.logistics.hub.domain.vo.HubRouteEdge;
import com.bonae.logistics.hub.presentation.dto.request.HubRouteUpdateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "eureka.client.enabled=false")
class HubRouteGraphCacheIntegrationTest {

    private static final String CACHE_NAME = "hubRouteGraph";
    private static final String CACHE_KEY = "active";
    private static final String REDIS_KEY = "hub:route-graph:active";

    @Autowired
    private HubRouteGraphProvider hubRouteGraphProvider;

    @Autowired
    private HubRouteService hubRouteService;

    @Autowired
    private HubService hubService;

    @Autowired
    private HubRepository hubRepository;

    @MockitoSpyBean
    private HubRouteRepository hubRouteRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Hub departureHub;
    private Hub arrivalHub;
    private HubRoute hubRoute;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();

        departureHub = Hub.create(
                "그래프캐시출발허브-" + suffix,
                "그래프캐시출발주소-" + suffix,
                37.4742027808565,
                127.123621185562
        );
        arrivalHub = Hub.create(
                "그래프캐시도착허브-" + suffix,
                "그래프캐시도착주소-" + suffix,
                37.1896213142136,
                127.375050006958
        );

        hubRepository.saveAndFlush(departureHub);
        hubRepository.saveAndFlush(arrivalHub);

        hubRoute = HubRoute.create(departureHub, arrivalHub);
        hubRouteRepository.saveAndFlush(hubRoute);

        evictCache();
    }

    @AfterEach
    void tearDown() {
        evictCache();

        if (hubRouteRepository.existsById(hubRoute.getId())) {
            hubRouteRepository.deleteById(hubRoute.getId());
            hubRouteRepository.flush();
        }

        if (hubRepository.existsById(departureHub.getId())) {
            hubRepository.deleteById(departureHub.getId());
        }
        if (hubRepository.existsById(arrivalHub.getId())) {
            hubRepository.deleteById(arrivalHub.getId());
        }

        hubRepository.flush();
    }

    @Test
    @DisplayName("캐시 미스 시 활성 간선을 반환하고 6시간 TTL로 캐시에 저장한다")
    void cachesActiveRoutesOnMiss() {
        List<HubRouteEdge> result = hubRouteGraphProvider.getActiveRoutes();

        HubRouteEdge expected = HubRouteEdge.from(hubRoute);
        assertThat(result).contains(expected);

        Cache.ValueWrapper cached = cache().get(CACHE_KEY);
        assertThat(cached).isNotNull();
        assertThat(cached.get()).isEqualTo(result);

        Long ttl = redisTemplate.getExpire(REDIS_KEY, TimeUnit.SECONDS);
        assertThat(ttl).isNotNull();
        assertThat(ttl).isBetween(21_590L, 21_600L);
    }

    @Test
    @DisplayName("캐시 적중 시 활성 간선을 DB에서 다시 조회하지 않는다")
    void skipsDbOnCacheHit() {
        hubRouteGraphProvider.getActiveRoutes();

        hubRouteGraphProvider.getActiveRoutes();

        verify(hubRouteRepository, times(1)).findAllByDeletedAtIsNull();
    }

    @Test
    @DisplayName("이동정보 수정 후 활성 간선 캐시를 무효화한다")
    void evictsCacheAfterRouteUpdate() {
        hubRouteGraphProvider.getActiveRoutes();
        assertThat(cache().get(CACHE_KEY)).isNotNull();

        hubRouteService.update(hubRoute.getId(), updateRequest(150_000, 7_200));

        assertThat(cache().get(CACHE_KEY)).isNull();
    }

    @Test
    @DisplayName("허브 삭제 후 활성 간선 캐시를 무효화한다")
    void evictsCacheAfterHubDelete() {
        hubRouteGraphProvider.getActiveRoutes();
        assertThat(cache().get(CACHE_KEY)).isNotNull();

        hubService.delete(departureHub.getId());

        assertThat(cache().get(CACHE_KEY)).isNull();
    }

    private HubRouteUpdateRequest updateRequest(int distanceMeters, int durationSeconds) {
        HubRouteUpdateRequest request = new HubRouteUpdateRequest();
        ReflectionTestUtils.setField(request, "distanceMeters", distanceMeters);
        ReflectionTestUtils.setField(request, "durationSeconds", durationSeconds);
        return request;
    }

    private Cache cache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            throw new IllegalStateException("활성 간선 그래프 캐시를 찾을 수 없습니다.");
        }
        return cache;
    }

    private void evictCache() {
        cache().evict(CACHE_KEY);
    }
}