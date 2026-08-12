package com.bonae.logistics.hub.application.service;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "eureka.client.enabled=false")
class HubServiceCacheFailureIntegrationTest {

    @DynamicPropertySource
    static void useUnavailableRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.port", () -> 1);
    }

    @Autowired
    private HubService hubService;

    @Autowired
    private HubRepository hubRepository;

    private Hub hub;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        hub = Hub.create("장애테스트허브-" + suffix, "장애테스트주소-" + suffix, 37.0, 127.0);
        hubRepository.saveAndFlush(hub);
    }

    @AfterEach
    void tearDown() {
        hubRepository.deleteById(hub.getId());
    }

    @Test
    @DisplayName("Redis 조회와 저장이 실패해도 DB 조회 결과를 반환한다")
    void fallsBackToDbWhenRedisIsUnavailable() {
        HubDetailResponse result = hubService.getHubDetail(hub.getId());

        assertThat(result.getHubId()).isEqualTo(hub.getId());
        assertThat(result.getName()).isEqualTo(hub.getName());
    }

    @Test
    @DisplayName("Redis 무효화가 실패해도 허브 수정은 정상 완료한다")
    void completesUpdateWhenCacheEvictionFails() {
        HubDetailResponse result = hubService.update(hub.getId(), updateRequestWithName("장애중수정"));

        assertThat(result.getName()).isEqualTo("장애중수정");
        assertThat(hubRepository.findByIdAndDeletedAtIsNull(hub.getId()))
                .get()
                .extracting(Hub::getName)
                .isEqualTo("장애중수정");
    }

    private HubUpdateRequest updateRequestWithName(String name) {
        HubUpdateRequest request = new HubUpdateRequest();
        ReflectionTestUtils.setField(request, "name", name);
        return request;
    }
}
