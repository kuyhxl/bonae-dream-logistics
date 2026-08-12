package com.bonae.logistics.company.domain;

import com.bonae.logistics.common.config.JpaAuditingConfig;
import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.bonae.logistics.company.domain.entity.Inventory;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.CompanyRepository;
import com.bonae.logistics.company.domain.repository.InventoryRepository;
import com.bonae.logistics.company.domain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 PostgreSQL(docker-compose)에 접속해 검증한다. 각 테스트는 @DataJpaTest가 걸어주는
// 트랜잭션 안에서 실행되고 종료 시 롤백되므로 DB에 데이터가 남지 않는다.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("save_유효한재고정보로_정상저장됨")
    void save_유효한재고정보로_정상저장됨() {
        Product product = productRepository.save(newProduct());
        UUID hubId = UUID.randomUUID();

        Inventory savedInventory = inventoryRepository.save(Inventory.create(product, hubId, 100));

        assertThat(savedInventory.getId()).isNotNull();
        assertThat(savedInventory.getProduct().getId()).isEqualTo(product.getId());
        assertThat(savedInventory.getHubId()).isEqualTo(hubId);
        assertThat(savedInventory.getQuantity()).isEqualTo(100);
        assertThat(savedInventory.getCreatedAt()).isNotNull();
        assertThat(savedInventory.getCreatedBy()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("existsByProduct_IdAndHubIdAndDeletedAtIsNull_동일상품과허브존재시_true반환")
    void existsByProduct_IdAndHubIdAndDeletedAtIsNull_동일상품과허브존재시_true반환() {
        Product product = productRepository.save(newProduct());
        UUID hubId = UUID.randomUUID();
        inventoryRepository.save(Inventory.create(product, hubId, 100));

        boolean exists = inventoryRepository.existsByProduct_IdAndHubIdAndDeletedAtIsNull(product.getId(), hubId);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByProduct_IdAndHubIdAndDeletedAtIsNull_다른허브면_false반환")
    void existsByProduct_IdAndHubIdAndDeletedAtIsNull_다른허브면_false반환() {
        Product product = productRepository.save(newProduct());
        inventoryRepository.save(Inventory.create(product, UUID.randomUUID(), 100));

        boolean exists =
                inventoryRepository.existsByProduct_IdAndHubIdAndDeletedAtIsNull(product.getId(), UUID.randomUUID());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByProduct_IdAndHubIdAndDeletedAtIsNull_동일조건재고가삭제된상태일때_false반환")
    void existsByProduct_IdAndHubIdAndDeletedAtIsNull_동일조건재고가삭제된상태일때_false반환() {
        Product product = productRepository.save(newProduct());
        UUID hubId = UUID.randomUUID();
        Inventory deletedInventory = Inventory.create(product, hubId, 100);
        deletedInventory.delete("tester");
        inventoryRepository.save(deletedInventory);

        boolean exists = inventoryRepository.existsByProduct_IdAndHubIdAndDeletedAtIsNull(product.getId(), hubId);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("delete_호출후flush하면_deletedAt과deletedBy가저장된다")
    void delete_호출후flush하면_deletedAt과deletedBy가저장된다() {
        Product product = productRepository.save(newProduct());
        Inventory inventory = inventoryRepository.saveAndFlush(Inventory.create(product, UUID.randomUUID(), 100));

        inventory.delete("tester");
        inventoryRepository.saveAndFlush(inventory);

        Inventory reloaded = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(reloaded.getDeletedAt()).isNotNull();
        assertThat(reloaded.getDeletedBy()).isEqualTo("tester");
        assertThat(reloaded.isDeleted()).isTrue();
    }

    private Product newProduct() {
        Company company = companyRepository.save(
                new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1"));
        return Product.create("갤럭시 스마트폰", company, new BigDecimal("1000.00"));
    }
}