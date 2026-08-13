package com.bonae.logistics.hub.application.service;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.hub.domain.entity.Hub;
import com.bonae.logistics.hub.domain.repository.HubRepository;
import com.bonae.logistics.hub.presentation.dto.request.HubCreateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "eureka.client.enabled=false")
class HubServiceConcurrencyIntegrationTest {

    private static final int THREAD_COUNT = 5;

    @Autowired
    private HubService hubService;

    @Autowired
    private HubRepository hubRepository;

    private final String duplicatedName = "동시성테스트허브-" + UUID.randomUUID();
    private final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

    @AfterEach
    void tearDown() throws InterruptedException {
        // 실패로 스레드가 남아 있는 채로 정리하면, 뒤늦게 커밋된 허브가 남는다.
        executor.shutdownNow();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        hubRepository.deleteAll(findCreatedHubs());
    }

    @Test
    @DisplayName("같은 이름으로 동시에 생성 요청해도 허브는 한 건만 저장된다")
    void createsOnlyOneHubWhenRequestedConcurrently() throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger duplicatedCount = new AtomicInteger();
        AtomicReference<Exception> unexpected = new AtomicReference<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            // 주소가 아닌 허브명 유니크 인덱스에서만 충돌하도록 주소는 스레드마다 다르게 만든다.
            HubCreateRequest request = createRequest("동시성테스트주소-" + i + "-" + UUID.randomUUID());

            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    hubService.create(request);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.HUB_NAME_DUPLICATED) {
                        duplicatedCount.incrementAndGet();
                    } else {
                        unexpected.compareAndSet(null, e);
                    }
                } catch (Exception e) {
                    unexpected.compareAndSet(null, e);
                } finally {
                    done.countDown();
                }
            });
        }

        // 모든 스레드가 대기 상태가 된 뒤 한 번에 출발시킨다.
        ready.await();
        start.countDown();
        boolean finished = done.await(10, TimeUnit.SECONDS);

        assertThat(finished).isTrue();
        assertThat(unexpected.get()).isNull();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(duplicatedCount.get()).isEqualTo(THREAD_COUNT - 1);
        assertThat(findCreatedHubs()).hasSize(1);
    }

    private HubCreateRequest createRequest(String address) {
        HubCreateRequest request = new HubCreateRequest();
        ReflectionTestUtils.setField(request, "name", duplicatedName);
        ReflectionTestUtils.setField(request, "address", address);
        ReflectionTestUtils.setField(request, "latitude", 37.0);
        ReflectionTestUtils.setField(request, "longitude", 127.0);
        return request;
    }

    private List<Hub> findCreatedHubs() {
        return hubRepository.findAll().stream()
                .filter(hub -> duplicatedName.equals(hub.getName()))
                .toList();
    }
}
