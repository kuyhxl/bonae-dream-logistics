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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    // 아래 검색 테스트들은 이 테스트가 만든 데이터끼리만 매칭되도록 각 테스트마다 새로 발급한 productId/hubId로 결과 범위를 좁힌다.
    // 실제 개발 DB(@AutoConfigureTestDatabase Replace.NONE)를 공유하는 환경이라
    // 다른 세션/수동 테스트로 남아있을 수 있는 기존 데이터와 우연히 겹쳐도 영향받지 않게 하기 위함이다.

    @Test
    @DisplayName("searchByProductIdAndHubIdAndDeletedAtIsNull_productId로필터링된다")
    void searchByProductIdAndHubIdAndDeletedAtIsNull_productId로필터링된다() {
        Product targetProduct = productRepository.save(newProduct());
        Product otherProduct = productRepository.save(newProduct("B"));
        Inventory targetInventory = inventoryRepository.save(Inventory.create(targetProduct, UUID.randomUUID(), 100));
        inventoryRepository.save(Inventory.create(otherProduct, UUID.randomUUID(), 50));
        Pageable pageable = PageRequest.of(0, 10);

        Page<Inventory> result = inventoryRepository.searchByProductIdAndHubIdAndDeletedAtIsNull(
                targetProduct.getId(), null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(targetInventory.getId());
    }

    @Test
    @DisplayName("searchByProductIdAndHubIdAndDeletedAtIsNull_hubId로필터링된다")
    void searchByProductIdAndHubIdAndDeletedAtIsNull_hubId로필터링된다() {
        UUID targetHubId = UUID.randomUUID();
        Product product1 = productRepository.save(newProduct());
        Product product2 = productRepository.save(newProduct("B"));
        Inventory targetInventory = inventoryRepository.save(Inventory.create(product1, targetHubId, 100));
        inventoryRepository.save(Inventory.create(product2, UUID.randomUUID(), 50));
        Pageable pageable = PageRequest.of(0, 10);

        Page<Inventory> result = inventoryRepository.searchByProductIdAndHubIdAndDeletedAtIsNull(
                null, targetHubId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(targetInventory.getId());
    }

    @Test
    @DisplayName("searchByProductIdAndHubIdAndDeletedAtIsNull_productId와hubId모두로필터링된다")
    void searchByProductIdAndHubIdAndDeletedAtIsNull_productId와hubId모두로필터링된다() {
        UUID targetHubId = UUID.randomUUID();
        Product targetProduct = productRepository.save(newProduct());
        // 같은 상품이라도 허브가 다르면 결과에서 제외되는지 함께 확인한다.
        inventoryRepository.save(Inventory.create(targetProduct, UUID.randomUUID(), 30));
        Inventory targetInventory = inventoryRepository.save(Inventory.create(targetProduct, targetHubId, 100));
        Pageable pageable = PageRequest.of(0, 10);

        Page<Inventory> result = inventoryRepository.searchByProductIdAndHubIdAndDeletedAtIsNull(
                targetProduct.getId(), targetHubId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(targetInventory.getId());
    }

    @Test
    @DisplayName("searchByProductIdAndHubIdAndDeletedAtIsNull_삭제된재고는_결과에서제외된다")
    void searchByProductIdAndHubIdAndDeletedAtIsNull_삭제된재고는_결과에서제외된다() {
        Product product = productRepository.save(newProduct());
        Inventory deletedInventory = Inventory.create(product, UUID.randomUUID(), 100);
        deletedInventory.delete("tester");
        inventoryRepository.save(deletedInventory);
        Pageable pageable = PageRequest.of(0, 10);

        Page<Inventory> result = inventoryRepository.searchByProductIdAndHubIdAndDeletedAtIsNull(
                product.getId(), null, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("searchByProductIdAndHubIdAndDeletedAtIsNull_조건이모두없으면_삭제되지않은재고가결과에포함된다")
    void searchByProductIdAndHubIdAndDeletedAtIsNull_조건이모두없으면_삭제되지않은재고가결과에포함된다() {
        Product product1 = productRepository.save(newProduct());
        Product product2 = productRepository.save(newProduct("B"));
        Inventory saved1 = inventoryRepository.save(Inventory.create(product1, UUID.randomUUID(), 100));
        Inventory saved2 = inventoryRepository.save(Inventory.create(product2, UUID.randomUUID(), 50));
        // 조건 없이 전체 조회하므로 기존 데이터와 섞일 수 있어, 정확한 개수 대신 이 둘이 포함되는지만 확인한다.
        Pageable pageable = PageRequest.of(0, 100);

        Page<Inventory> result = inventoryRepository.searchByProductIdAndHubIdAndDeletedAtIsNull(
                null, null, pageable);

        assertThat(result.getContent()).extracting(Inventory::getId)
                .contains(saved1.getId(), saved2.getId());
    }

    private Product newProduct() {
        return newProduct("A");
    }

    // 한 테스트 안에서 서로 다른 상품이 필요한 경우, 업체명+주소 유니크 제약(ux_p_companies_name_address_active)에
    // 걸리지 않도록 suffix로 구분되는 업체를 새로 만들어 상품을 발급한다.
    private Product newProduct(String suffix) {
        Company company = companyRepository.save(
                new Company("배송센터" + suffix, CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 " + suffix));
        return Product.create("갤럭시 스마트폰", company, new BigDecimal("1000.00"));
    }
}