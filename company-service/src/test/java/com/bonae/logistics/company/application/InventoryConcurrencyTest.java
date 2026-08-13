package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.bonae.logistics.company.domain.entity.Inventory;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.CompanyRepository;
import com.bonae.logistics.company.domain.repository.InventoryIdempotencyKeyRepository;
import com.bonae.logistics.company.domain.repository.InventoryRepository;
import com.bonae.logistics.company.domain.repository.ProductRepository;
import com.bonae.logistics.company.presentation.dto.request.ReqUpdateInventoryDto;
import com.bonae.logistics.company.presentation.dto.response.ResUpdateInventoryDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 PostgreSQL(docker-compose)에 붙여 여러 스레드(=여러 커넥션/트랜잭션)가 같은 재고 row를
// 동시에 건드릴 때도 원자적 UPDATE(WHERE 조건)와 멱등성 키 유니크 인덱스가 정합성을 지키는지 검증한다.
// InventoryRepositoryTest(@DataJpaTest)와 달리 트랜잭션 롤백에 기대지 않는다
// - 각 스레드가 updateInventory 호출마다 독립된 트랜잭션/커넥션을 열어야 실제 락 경합을 재현할 수 있기 때문에,
//    테스트 데이터를 직접 커밋하고 @AfterEach에서 수동으로 정리한다.
@SpringBootTest(properties = "eureka.client.enabled=false")
class InventoryConcurrencyTest {

    private static final int THREAD_COUNT = 20;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryIdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private Company company;
    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(new Company(
                "동시성테스트업체-" + UUID.randomUUID(), CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1"));
        product = productRepository.save(Product.create("동시성테스트상품", company, new BigDecimal("1000.00")));
    }

    @AfterEach
    void tearDown() {
        idempotencyKeyRepository.deleteAll(idempotencyKeyRepository.findAllByProductId(product.getId()));
        inventoryRepository.deleteById(inventory.getId());
        productRepository.deleteById(product.getId());
        companyRepository.deleteById(company.getId());
    }

    @Test
    @DisplayName("동시에서로다른주문으로DECREASE요청시_정확히요청한만큼만차감된다")
    void 동시에서로다른주문으로DECREASE요청시_정확히차감된다() throws InterruptedException {
        int initialQuantity = 100;
        inventory = inventoryRepository.saveAndFlush(Inventory.create(product, UUID.randomUUID(), initialQuantity));
        List<UUID> orderIds = distinctOrderIds();

        List<ResUpdateInventoryDto> responses =
                runConcurrently(orderIds, orderId -> updateReqDto(orderId, 1, "DECREASE"), null, null);

        assertThat(responses).hasSize(THREAD_COUNT);
        Inventory result = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(result.getQuantity()).isEqualTo(initialQuantity - THREAD_COUNT);
        assertThat(idempotencyKeyRepository.findAllByProductId(product.getId())).hasSize(THREAD_COUNT);
    }

    @Test
    @DisplayName("동시요청수량합이재고보다많을때_재고를초과해차감되지않고음수가되지않는다")
    void 동시요청수량합이재고보다많을때_초과차감되지않는다() throws InterruptedException {
        int initialQuantity = 10;
        inventory = inventoryRepository.saveAndFlush(Inventory.create(product, UUID.randomUUID(), initialQuantity));
        List<UUID> orderIds = distinctOrderIds();
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger shortageCount = new AtomicInteger();

        runConcurrently(orderIds, orderId -> updateReqDto(orderId, 1, "DECREASE"), successCount, shortageCount);

        assertThat(successCount.get() + shortageCount.get()).isEqualTo(THREAD_COUNT);
        assertThat(successCount.get()).isEqualTo(initialQuantity);
        assertThat(shortageCount.get()).isEqualTo(THREAD_COUNT - initialQuantity);
        Inventory result = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(result.getQuantity()).isEqualTo(0); // 절대 음수가 되지 않는다
    }

    @Test
    @DisplayName("같은주문으로DECREASE요청이동시에들어와도_정확히한번만반영되고모든응답이동일하다")
    void 같은주문으로동시요청시_한번만반영된다() throws InterruptedException {
        int initialQuantity = 100;
        inventory = inventoryRepository.saveAndFlush(Inventory.create(product, UUID.randomUUID(), initialQuantity));
        UUID sameOrderId = UUID.randomUUID();
        List<UUID> orderIds = IntStream.range(0, THREAD_COUNT).mapToObj(i -> sameOrderId).collect(Collectors.toList());

        List<ResUpdateInventoryDto> responses =
                runConcurrently(orderIds, orderId -> updateReqDto(orderId, 5, "DECREASE"), null, null);

        assertThat(responses).hasSize(THREAD_COUNT);
        assertThat(idempotencyKeyRepository.findAllByProductId(product.getId())).hasSize(1); // 딱 1건만 선점됨
        Inventory result = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(result.getQuantity()).isEqualTo(initialQuantity - 5); // 실제 반영은 1번만
        assertThat(responses).allSatisfy(res -> {
            assertThat(res.getBeforeQuantity()).isEqualTo(initialQuantity);
            assertThat(res.getAfterQuantity()).isEqualTo(initialQuantity - 5);
            assertThat(res.getChangedQuantity()).isEqualTo(5);
        });
    }

    private List<UUID> distinctOrderIds() {
        return IntStream.range(0, THREAD_COUNT).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toList());
    }

    // orderIds 개수만큼 스레드를 만들어 동시에 updateInventory를 호출한다.
    // CountDownLatch로 모든 스레드가 준비될 때까지 대기시켰다가 한 번에 풀어 경합 타이밍을 최대한 맞춘다.
    // successCount/shortageCount가 null이 아니면 정상/재고부족 결과를 각각 집계하고, 그 외 예외는 즉시 전파한다.
    private List<ResUpdateInventoryDto> runConcurrently(List<UUID> orderIds,
                                                          Function<UUID, ReqUpdateInventoryDto> reqDtoFactory,
                                                          AtomicInteger successCount,
                                                          AtomicInteger shortageCount) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(orderIds.size());
        CountDownLatch readyLatch = new CountDownLatch(orderIds.size());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(orderIds.size());
        List<ResUpdateInventoryDto> responses = new CopyOnWriteArrayList<>();
        // executor.submit(Runnable)이 반환하는 Future를 확인하지 않으면 스레드 안에서 던진 예외가
        // 조용히 사라지므로, 예상치 못한 예외는 직접 모아뒀다가 아래에서 명시적으로 검증한다.
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();

        for (UUID orderId : orderIds) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    ResUpdateInventoryDto res = inventoryService.updateInventory(inventory.getId(), reqDtoFactory.apply(orderId));
                    responses.add(res);
                    if (successCount != null) {
                        successCount.incrementAndGet();
                    }
                } catch (BusinessException e) {
                    if (shortageCount != null && e.getErrorCode() == ErrorCode.STOCK_SHORTAGE) {
                        shortageCount.incrementAndGet();
                    } else {
                        unexpectedErrors.add(e);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(completed).as("모든 스레드가 제한 시간 내에 끝나야 한다").isTrue();
        assertThat(unexpectedErrors).as("예상치 못한 예외 발생: %s", unexpectedErrors).isEmpty();

        return responses;
    }

    private ReqUpdateInventoryDto updateReqDto(UUID orderId, Integer quantity, String type) {
        return ReqUpdateInventoryDto.builder()
                .orderId(orderId)
                .quantity(quantity)
                .type(type)
                .build();
    }
}