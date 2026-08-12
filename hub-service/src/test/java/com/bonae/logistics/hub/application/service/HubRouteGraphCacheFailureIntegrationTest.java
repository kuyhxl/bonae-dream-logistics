package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.hub.domain.entity.Hub;
import com.bonae.logistics.hub.domain.entity.HubRoute;
import com.bonae.logistics.hub.domain.repository.HubRepository;
import com.bonae.logistics.hub.domain.repository.HubRouteRepository;
import com.bonae.logistics.hub.domain.vo.HubRouteEdge;
import com.bonae.logistics.hub.presentation.dto.request.HubRouteUpdateRequest;
import com.bonae.logistics.hub.presentation.dto.response.HubRouteDetailResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "eureka.client.enabled=false")
class HubRouteGraphCacheFailureIntegrationTest {

    @DynamicPropertySource
    static void useUnavailableRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.port", () -> 1);
    }

    @Autowired
    private HubRouteGraphProvider hubRouteGraphProvider;

    @Autowired
    private HubRepository hubRepository;

    @Autowired
    private HubRouteRepository hubRouteRepository;

    @Autowired
    private HubRouteService hubRouteService;

    private Hub departureHub;
    private Hub arrivalHub;
    private HubRoute hubRoute;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();

        departureHub = Hub.create(
                "그래프장애출발허브-" + suffix,
                "그래프장애출발주소-" + suffix,
                37.4742027808565,
                127.123621185562
        );
        arrivalHub = Hub.create(
                "그래프장애도착허브-" + suffix,
                "그래프장애도착주소-" + suffix,
                37.1896213142136,
                127.375050006958
        );

        hubRepository.saveAndFlush(departureHub);
        hubRepository.saveAndFlush(arrivalHub);

        hubRoute = HubRoute.create(departureHub, arrivalHub);
        hubRouteRepository.saveAndFlush(hubRoute);
    }

    @AfterEach
    void tearDown() {
        hubRouteRepository.deleteById(hubRoute.getId());
        hubRouteRepository.flush();

        hubRepository.deleteById(departureHub.getId());
        hubRepository.deleteById(arrivalHub.getId());
        hubRepository.flush();
    }

    @Test
    @DisplayName("Redis 조회와 저장이 실패해도 DB의 활성 간선을 반환한다")
    void returnsActiveRoutesWhenRedisIsUnavailable() {
        List<HubRouteEdge> result = hubRouteGraphProvider.getActiveRoutes();

        assertThat(result).contains(HubRouteEdge.from(hubRoute));
    }

    @Test
    @DisplayName("Redis 무효화가 실패해도 이동정보 수정은 정상 완료한다")
    void completesUpdateWhenCacheEvictionFails() {
        HubRouteDetailResponse result = hubRouteService.update(hubRoute.getId(), updateRequest(150_000, 7_200));

        assertThat(result.getDistanceMeters()).isEqualTo(150_000);
        assertThat(result.getDurationSeconds()).isEqualTo(7_200);

        HubRoute updated = hubRouteRepository.findByIdAndDeletedAtIsNull(hubRoute.getId()).orElseThrow();
        assertThat(updated.getDistanceMeters()).isEqualTo(150_000);
        assertThat(updated.getDurationSeconds()).isEqualTo(7_200);
    }

    private HubRouteUpdateRequest updateRequest(int distanceMeters, int durationSeconds) {
        HubRouteUpdateRequest request = new HubRouteUpdateRequest();
        ReflectionTestUtils.setField(request, "distanceMeters", distanceMeters);
        ReflectionTestUtils.setField(request, "durationSeconds", durationSeconds);
        return request;
    }
}