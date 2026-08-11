package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.hub.domain.entity.Hub;
import com.bonae.logistics.hub.domain.repository.HubRepository;
import com.bonae.logistics.hub.presentation.dto.request.HubUpdateRequest;
import com.bonae.logistics.hub.presentation.dto.response.HubDetailResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "eureka.client.enabled=false")
class HubServiceCacheIntegrationTest {

    private static final String CACHE_NAME = "hubDetail";

    @Autowired
    private HubService hubService;

    @MockitoSpyBean
    private HubRepository hubRepository;

    @Autowired
    private CacheManager cacheManager;

    private Hub hub;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        hub = Hub.create("캐시테스트허브-" + suffix, "캐시테스트주소-" + suffix, 37.0, 127.0);
        hubRepository.saveAndFlush(hub);
        evictCache(hub.getId());
    }

    @AfterEach
    void tearDown() {
        evictCache(hub.getId());
        hubRepository.deleteById(hub.getId());
    }

    @Test
    @DisplayName("캐시 미스 시 DB 조회 결과를 반환하고 캐시에 저장한다")
    void cachesResultOnMiss() {
        HubDetailResponse result = hubService.getHubDetail(hub.getId());

        assertThat(result.getHubId()).isEqualTo(hub.getId());

        Cache.ValueWrapper cached = cache().get(hub.getId());
        assertThat(cached).isNotNull();
        assertThat(cached.get()).isInstanceOf(HubDetailResponse.class);
        assertThat(((HubDetailResponse) cached.get()).getHubId()).isEqualTo(hub.getId());
    }

    @Test
    @DisplayName("캐시 적중 시 DB를 다시 조회하지 않는다")
    void skipsDbOnHit() {
        hubService.getHubDetail(hub.getId());

        hubService.getHubDetail(hub.getId());

        verify(hubRepository, times(1)).findByIdAndDeletedAtIsNull(hub.getId());
    }

    @Test
    @DisplayName("수정 커밋 후 캐시를 무효화하고 최신 값을 조회한다")
    void evictsCacheAfterUpdate() {
        hubService.getHubDetail(hub.getId());
        assertThat(cache().get(hub.getId())).isNotNull();

        hubService.update(hub.getId(), updateRequestWithName("수정된 허브명"));

        assertThat(cache().get(hub.getId())).isNull();
        HubDetailResponse result = hubService.getHubDetail(hub.getId());
        assertThat(result.getName()).isEqualTo("수정된 허브명");
    }

    @Test
    @DisplayName("삭제 커밋 후 캐시를 무효화하고 삭제된 허브를 조회하지 않는다")
    void evictsCacheAfterDelete() {
        hubService.getHubDetail(hub.getId());
        assertThat(cache().get(hub.getId())).isNotNull();

        hubService.delete(hub.getId());

        assertThat(cache().get(hub.getId())).isNull();
        assertThatThrownBy(() -> hubService.getHubDetail(hub.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.HUB_NOT_FOUND));
    }

    private HubUpdateRequest updateRequestWithName(String name) {
        HubUpdateRequest request = new HubUpdateRequest();
        ReflectionTestUtils.setField(request, "name", name);
        return request;
    }

    private Cache cache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            throw new IllegalStateException("허브 단건 캐시를 찾을 수 없습니다.");
        }
        return cache;
    }

    private void evictCache(UUID hubId) {
        cache().evict(hubId);
    }
}
