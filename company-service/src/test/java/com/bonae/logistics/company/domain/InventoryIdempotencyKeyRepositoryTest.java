package com.bonae.logistics.company.domain;

import com.bonae.logistics.common.config.JpaAuditingConfig;
import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.bonae.logistics.company.domain.entity.InventoryChangeType;
import com.bonae.logistics.company.domain.entity.InventoryIdempotencyKey;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.CompanyRepository;
import com.bonae.logistics.company.domain.repository.InventoryIdempotencyKeyRepository;
import com.bonae.logistics.company.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 PostgreSQL(docker-compose)에 접속해 검증한다. 각 테스트는 @DataJpaTest가 걸어주는
// 트랜잭션 안에서 실행되고 종료 시 롤백되므로 DB에 데이터가 남지 않는다.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class InventoryIdempotencyKeyRepositoryTest {

    @Autowired
    private InventoryIdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("tryInsert_최초요청이면_1을반환하고행이생성된다")
    void tryInsert_최초요청이면_1반환() {
        Product product = productRepository.save(newProduct());
        UUID orderId = UUID.randomUUID();

        int inserted = idempotencyKeyRepository.tryInsert(
                UUID.randomUUID(), orderId, product.getId(), InventoryChangeType.DECREASE.name());

        assertThat(inserted).isEqualTo(1);
        Optional<InventoryIdempotencyKey> saved = idempotencyKeyRepository
                .findByOrderIdAndProductIdAndOperation(orderId, product.getId(), InventoryChangeType.DECREASE);
        assertThat(saved).isPresent();
        assertThat(saved.get().getCreatedAt()).isNotNull();
        // 선점 시점엔 아직 반영 전이라 before/after는 비어 있어야 한다.
        assertThat(saved.get().getBeforeQuantity()).isNull();
        assertThat(saved.get().getAfterQuantity()).isNull();
    }

    @Test
    @DisplayName("tryInsert_같은조합으로재요청하면_0을반환하고중복행이생기지않는다")
    void tryInsert_같은조합재요청_0반환() {
        Product product = productRepository.save(newProduct());
        UUID orderId = UUID.randomUUID();
        idempotencyKeyRepository.tryInsert(
                UUID.randomUUID(), orderId, product.getId(), InventoryChangeType.DECREASE.name());

        int secondInsert = idempotencyKeyRepository.tryInsert(
                UUID.randomUUID(), orderId, product.getId(), InventoryChangeType.DECREASE.name());

        assertThat(secondInsert).isEqualTo(0);
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("tryInsert_operation이다르면_별개조합으로처리된다")
    void tryInsert_operation이다르면_별개로처리() {
        Product product = productRepository.save(newProduct());
        UUID orderId = UUID.randomUUID();
        int decreaseInserted = idempotencyKeyRepository.tryInsert(
                UUID.randomUUID(), orderId, product.getId(), InventoryChangeType.DECREASE.name());

        int restoreInserted = idempotencyKeyRepository.tryInsert(
                UUID.randomUUID(), orderId, product.getId(), InventoryChangeType.RESTORE.name());

        assertThat(decreaseInserted).isEqualTo(1);
        assertThat(restoreInserted).isEqualTo(1);
        assertThat(idempotencyKeyRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("fillSnapshot_호출하면_beforeQuantity와afterQuantity가채워진다")
    void fillSnapshot_호출하면_before와after가채워진다() {
        Product product = productRepository.save(newProduct());
        UUID orderId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();
        idempotencyKeyRepository.tryInsert(keyId, orderId, product.getId(), InventoryChangeType.DECREASE.name());

        idempotencyKeyRepository.fillSnapshot(keyId, 100, 70);

        InventoryIdempotencyKey saved = idempotencyKeyRepository
                .findByOrderIdAndProductIdAndOperation(orderId, product.getId(), InventoryChangeType.DECREASE)
                .orElseThrow();
        assertThat(saved.getId()).isEqualTo(keyId);
        assertThat(saved.getBeforeQuantity()).isEqualTo(100);
        assertThat(saved.getAfterQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("findByOrderIdAndProductIdAndOperation_존재하지않는조합이면_빈값반환")
    void findByOrderIdAndProductIdAndOperation_존재하지않으면_빈값반환() {
        Optional<InventoryIdempotencyKey> found = idempotencyKeyRepository
                .findByOrderIdAndProductIdAndOperation(UUID.randomUUID(), UUID.randomUUID(), InventoryChangeType.DECREASE);

        assertThat(found).isEmpty();
    }

    private Product newProduct() {
        Company company = companyRepository.save(
                new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1"));
        return Product.create("갤럭시 스마트폰", company, new BigDecimal("1000.00"));
    }
}