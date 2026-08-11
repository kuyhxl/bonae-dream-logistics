package com.bonae.logistics.company.domain;

import com.bonae.logistics.common.config.JpaAuditingConfig;
import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.CompanyRepository;
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
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("save_유효한상품정보로_정상저장됨")
    void save_유효한상품정보로_정상저장됨() {
        Company company = companyRepository.save(
                new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1"));
        Product product = Product.create("갤럭시 스마트폰", company, new BigDecimal("1200000.00"));

        Product savedProduct = productRepository.save(product);

        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getName()).isEqualTo("갤럭시 스마트폰");
        assertThat(savedProduct.getCompany().getId()).isEqualTo(company.getId());
        assertThat(savedProduct.getPrice()).isEqualByComparingTo("1200000.00");
        assertThat(savedProduct.getCreatedAt()).isNotNull();
        assertThat(savedProduct.getCreatedBy()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("existsByNameAndCompany_IdAndDeletedAtIsNull_동일이름과업체존재시_true반환")
    void existsByNameAndCompany_IdAndDeletedAtIsNull_동일이름과업체존재시_true반환() {
        Company company = companyRepository.save(
                new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1"));
        productRepository.save(Product.create("갤럭시 스마트폰", company, new BigDecimal("1000.00")));

        boolean exists = productRepository.existsByNameAndCompany_IdAndDeletedAtIsNull("갤럭시 스마트폰", company.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByNameAndCompany_IdAndDeletedAtIsNull_다른업체의동일상품명이면_false반환")
    void existsByNameAndCompany_IdAndDeletedAtIsNull_다른업체의동일상품명이면_false반환() {
        Company company = companyRepository.save(
                new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1"));
        Company anotherCompany = companyRepository.save(
                new Company("배송센터B", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 송파구 올림픽로 1"));
        productRepository.save(Product.create("갤럭시 스마트폰", company, new BigDecimal("1000.00")));

        boolean exists =
                productRepository.existsByNameAndCompany_IdAndDeletedAtIsNull("갤럭시 스마트폰", anotherCompany.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByNameAndCompany_IdAndDeletedAtIsNull_동일조건상품이삭제된상태일때_false반환")
    void existsByNameAndCompany_IdAndDeletedAtIsNull_동일조건상품이삭제된상태일때_false반환() {
        Company company = companyRepository.save(
                new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1"));
        Product deletedProduct = Product.create("갤럭시 스마트폰", company, new BigDecimal("1000.00"));
        deletedProduct.delete("tester");
        productRepository.save(deletedProduct);

        boolean exists = productRepository.existsByNameAndCompany_IdAndDeletedAtIsNull("갤럭시 스마트폰", company.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("delete_호출후flush하면_deletedAt과deletedBy가저장된다")
    void delete_호출후flush하면_deletedAt과deletedBy가저장된다() {
        Company company = companyRepository.save(
                new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1"));
        Product product = productRepository.saveAndFlush(
                Product.create("갤럭시 스마트폰", company, new BigDecimal("1000.00")));

        product.delete("tester");
        productRepository.saveAndFlush(product);

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getDeletedAt()).isNotNull();
        assertThat(reloaded.getDeletedBy()).isEqualTo("tester");
        assertThat(reloaded.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull_삭제되지않은상품을_정상조회됨")
    void findByIdAndDeletedAtIsNull_삭제되지않은상품을_정상조회됨() {
        Company company = companyRepository.save(
                new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1"));
        Product savedProduct = productRepository.save(
                Product.create("갤럭시 스마트폰", company, new BigDecimal("1200000.00")));

        var foundProduct = productRepository.findByIdAndDeletedAtIsNull(savedProduct.getId());

        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getName()).isEqualTo("갤럭시 스마트폰");
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull_삭제된상품은_빈값반환")
    void findByIdAndDeletedAtIsNull_삭제된상품은_빈값반환() {
        Company company = companyRepository.save(
                new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1"));
        Product deletedProduct = Product.create("갤럭시 스마트폰", company, new BigDecimal("1200000.00"));
        deletedProduct.delete("tester");
        Product savedProduct = productRepository.save(deletedProduct);

        var foundProduct = productRepository.findByIdAndDeletedAtIsNull(savedProduct.getId());

        assertThat(foundProduct).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull_존재하지않는id일때_빈값반환")
    void findByIdAndDeletedAtIsNull_존재하지않는id일때_빈값반환() {
        var foundProduct = productRepository.findByIdAndDeletedAtIsNull(UUID.randomUUID());

        assertThat(foundProduct).isEmpty();
    }
}