package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.hub.domain.entity.Hub;
import com.bonae.logistics.hub.domain.entity.HubRoute;
import com.bonae.logistics.hub.domain.repository.HubRepository;
import com.bonae.logistics.hub.domain.repository.HubRouteRepository;
import com.bonae.logistics.hub.presentation.dto.request.HubRouteUpdateRequest;
import com.bonae.logistics.hub.presentation.dto.response.HubRoutePathResponse;
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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "eureka.client.enabled=false")
class HubRoutePathCacheIntegrationTest {

    private static final String CACHE_NAME = "hubPath";

    @Autowired
    private HubRoutePathService hubRoutePathService;

    @Autowired
    private HubRouteService hubRouteService;

    @Autowired
    private HubService hubService;

    @Autowired
    private HubRepository hubRepository;

    @Autowired
    private HubRouteRepository hubRouteRepository;

    @MockitoSpyBean
    private HubRoutePathFinder hubRoutePathFinder;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Hub departureHub;
    private Hub arrivalHub;
    private HubRoute forwardRoute;
    private HubRoute reverseRoute;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();

        // 양방향 키 분리를 검증할 수 있도록 반대 방향 이동정보도 함께 생성한다.
        departureHub = Hub.create("경로캐시출발허브-" + suffix, "경로캐시출발주소-" + suffix, 37.4742027808565, 127.123621185562);
        arrivalHub = Hub.create("경로캐시도착허브-" + suffix, "경로캐시도착주소-" + suffix, 37.1896213142136, 127.375050006958);

        hubRepository.saveAndFlush(departureHub);
        hubRepository.saveAndFlush(arrivalHub);

        forwardRoute = HubRoute.create(departureHub, arrivalHub);
        reverseRoute = HubRoute.create(arrivalHub, departureHub);

        hubRouteRepository.saveAndFlush(forwardRoute);
        hubRouteRepository.saveAndFlush(reverseRoute);

        evictPathCache();
        evictGraphCache();
    }

    @AfterEach
    void tearDown() {
        evictPathCache();
        evictGraphCache();

        if (hubRouteRepository.existsById(forwardRoute.getId())) {
            hubRouteRepository.deleteById(forwardRoute.getId());
        }
        if (hubRouteRepository.existsById(reverseRoute.getId())) {
            hubRouteRepository.deleteById(reverseRoute.getId());
        }
        hubRouteRepository.flush();

        if (hubRepository.existsById(departureHub.getId())) {
            hubRepository.deleteById(departureHub.getId());
        }
        if (hubRepository.existsById(arrivalHub.getId())) {
            hubRepository.deleteById(arrivalHub.getId());
        }
        hubRepository.flush();
    }

    @Test
    @DisplayName("캐시 미스 시 최적 경로를 계산하고 5분 TTL로 저장한다")
    void cachesPathResultOnMiss() {
        HubRoutePathResponse result = hubRoutePathService.findShortestPath(departureHub.getId(), arrivalHub.getId());

        assertThat(result.getSegments()).hasSize(1);
        assertThat(result.getSegments().get(0).getFromHubId()).isEqualTo(departureHub.getId());
        assertThat(result.getSegments().get(0).getToHubId()).isEqualTo(arrivalHub.getId());

        Cache.ValueWrapper cached = pathCache().get(pathKey(departureHub.getId(), arrivalHub.getId()));
        assertThat(cached).isNotNull();
        assertThat(cached.get()).isInstanceOf(HubRoutePathResponse.class);

        // 5분은 300초이며 명령 실행 시간을 고려해 10초의 허용 범위를 둔다.
        Long ttl = redisTemplate.getExpire(redisPathKey(departureHub.getId(), arrivalHub.getId()), TimeUnit.SECONDS);
        assertThat(ttl).isNotNull();
        assertThat(ttl).isBetween(290L, 300L);
    }

    @Test
    @DisplayName("캐시 적중 시 다익스트라 경로 계산을 다시 실행하지 않는다")
    void skipsPathCalculationOnCacheHit() {
        hubRoutePathService.findShortestPath(departureHub.getId(), arrivalHub.getId());

        hubRoutePathService.findShortestPath(departureHub.getId(), arrivalHub.getId());

        verify(hubRoutePathFinder, times(1)).findShortestPath(
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.eq(departureHub.getId()),
                org.mockito.ArgumentMatchers.eq(arrivalHub.getId())
        );
    }

    @Test
    @DisplayName("반대 방향 경로는 별도의 캐시 키로 저장한다")
    void usesDifferentKeysForOppositeDirections() {
        hubRoutePathService.findShortestPath(departureHub.getId(), arrivalHub.getId());
        hubRoutePathService.findShortestPath(arrivalHub.getId(), departureHub.getId());

        assertThat(pathCache().get(pathKey(departureHub.getId(), arrivalHub.getId()))).isNotNull();
        assertThat(pathCache().get(pathKey(arrivalHub.getId(), departureHub.getId()))).isNotNull();
        assertThat(redisTemplate.hasKey(redisPathKey(departureHub.getId(), arrivalHub.getId()))).isTrue();
        assertThat(redisTemplate.hasKey(redisPathKey(arrivalHub.getId(), departureHub.getId()))).isTrue();
    }

    @Test
    @DisplayName("이동정보 수정 후 최적 경로 캐시 전체를 무효화한다")
    void evictsPathCacheAfterRouteUpdate() {
        hubRoutePathService.findShortestPath(departureHub.getId(), arrivalHub.getId());
        hubRoutePathService.findShortestPath(arrivalHub.getId(), departureHub.getId());

        hubRouteService.update(forwardRoute.getId(), updateRequest(150_000, 7_200));

        assertThat(pathCache().get(pathKey(departureHub.getId(), arrivalHub.getId()))).isNull();
        assertThat(pathCache().get(pathKey(arrivalHub.getId(), departureHub.getId()))).isNull();
    }

    @Test
    @DisplayName("허브 삭제 후 최적 경로 캐시 전체를 무효화한다")
    void evictsPathCacheAfterHubDelete() {
        hubRoutePathService.findShortestPath(departureHub.getId(), arrivalHub.getId());
        hubRoutePathService.findShortestPath(arrivalHub.getId(), departureHub.getId());

        hubService.delete(departureHub.getId());

        assertThat(pathCache().get(pathKey(departureHub.getId(), arrivalHub.getId()))).isNull();
        assertThat(pathCache().get(pathKey(arrivalHub.getId(), departureHub.getId()))).isNull();
    }

    private HubRouteUpdateRequest updateRequest(int distanceMeters, int durationSeconds) {
        HubRouteUpdateRequest request = new HubRouteUpdateRequest();
        ReflectionTestUtils.setField(request, "distanceMeters", distanceMeters);
        ReflectionTestUtils.setField(request, "durationSeconds", durationSeconds);
        return request;
    }

    private Cache pathCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);

        if (cache == null) {
            throw new IllegalStateException("최적 경로 결과 캐시를 찾을 수 없습니다.");
        }

        return cache;
    }

    private void evictPathCache() {
        pathCache().clear();
    }

    private void evictGraphCache() {
        Cache cache = cacheManager.getCache("hubRouteGraph");

        if (cache == null) {
            throw new IllegalStateException("활성 간선 그래프 캐시를 찾을 수 없습니다.");
        }

        cache.evict("active");
    }

    private String pathKey(UUID departureHubId, UUID arrivalHubId) {
        return departureHubId + ":" + arrivalHubId;
    }

    private String redisPathKey(UUID departureHubId, UUID arrivalHubId) {
        return "hub:path:" + pathKey(departureHubId, arrivalHubId);
    }
}
