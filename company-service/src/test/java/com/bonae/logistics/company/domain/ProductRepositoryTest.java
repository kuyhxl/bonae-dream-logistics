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

    // 이 브랜치가 생성한 상품끼리만 매칭되도록 매 테스트마다 새로 발급한 hubId로 업체를 만들어
    // 실제 개발 DB(@AutoConfigureTestDatabase Replace.NONE)에 남아있는 다른 세션의 데이터와 섞이지 않게 한다.
    @Test
    @DisplayName("findAllByDeletedAtIsNull_삭제되지않은상품만_조회된다")
    void findAllByDeletedAtIsNull_삭제되지않은상품만_조회된다() {
        Company company = companyRepository.save(
                new Company("삭제조회테스트업체", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1"));
        Product activeProduct = productRepository.save(
                Product.create("삭제조회테스트상품", company, new BigDecimal("1000.00")));
        Product deletedProduct = Product.create("삭제조회테스트상품(삭제)", company, new BigDecimal("1000.00"));
        deletedProduct.delete("tester");
        productRepository.save(deletedProduct);
        Pageable pageable = PageRequest.of(0, 100);

        Page<Product> result = productRepository.findAllByDeletedAtIsNull(pageable);

        assertThat(result.getContent()).extracting(Product::getId).contains(activeProduct.getId());
        assertThat(result.getContent()).extracting(Product::getId).doesNotContain(deletedProduct.getId());
    }
}