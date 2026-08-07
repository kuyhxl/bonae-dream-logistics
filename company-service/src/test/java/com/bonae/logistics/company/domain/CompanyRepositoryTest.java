package com.bonae.logistics.company.domain;

import com.bonae.logistics.common.config.JpaAuditingConfig;
import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.bonae.logistics.company.domain.repository.CompanyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 PostgreSQL(docker-compose)에 접속해 검증한다. 각 테스트는 @DataJpaTest가 걸어주는
// 트랜잭션 안에서 실행되고 종료 시 롤백되므로 DB에 데이터가 남지 않는다.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class CompanyRepositoryTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("save_유효한업체정보로_정상저장됨")
    void save_유효한업체정보로_정상저장됨() {
        Company company = new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");

        Company savedCompany = companyRepository.save(company);

        assertThat(savedCompany.getId()).isNotNull();
        assertThat(savedCompany.getName()).isEqualTo("배송센터A");
        assertThat(savedCompany.getType()).isEqualTo(CompanyType.PRODUCER);
        assertThat(savedCompany.getCreatedAt()).isNotNull();
        assertThat(savedCompany.getCreatedBy()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("findById_저장된업체를_정상조회됨")
    void findById_저장된업체를_정상조회됨() {
        Company savedCompany = companyRepository.save(
                new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1")
        );

        var foundCompany = companyRepository.findById(savedCompany.getId());

        assertThat(foundCompany).isPresent();
        assertThat(foundCompany.get().getName()).isEqualTo("배송센터A");
    }

    @Test
    @DisplayName("existsByNameAndAddressAndDeletedAtIsNull_동일한이름과주소존재시_true반환")
    void existsByNameAndAddressAndDeletedAtIsNull_동일한이름과주소존재시_true반환() {
        companyRepository.save(new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1"));

        boolean exists = companyRepository.existsByNameAndAddressAndDeletedAtIsNull("배송센터A", "서울시 강남구 테헤란로 1");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByNameAndAddressAndDeletedAtIsNull_이름또는주소가다를때_false반환")
    void existsByNameAndAddressAndDeletedAtIsNull_이름또는주소가다를때_false반환() {
        companyRepository.save(new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1"));

        boolean existsWithDifferentName =
                companyRepository.existsByNameAndAddressAndDeletedAtIsNull("배송센터B", "서울시 강남구 테헤란로 1");
        boolean existsWithDifferentAddress =
                companyRepository.existsByNameAndAddressAndDeletedAtIsNull("배송센터A", "서울시 송파구 올림픽로 1");

        assertThat(existsWithDifferentName).isFalse();
        assertThat(existsWithDifferentAddress).isFalse();
    }

    @Test
    @DisplayName("existsByNameAndAddressAndDeletedAtIsNull_동일조건업체가삭제된상태일때_false반환")
    void existsByNameAndAddressAndDeletedAtIsNull_동일조건업체가삭제된상태일때_false반환() {
        Company deletedCompany = new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        deletedCompany.delete("tester");
        companyRepository.save(deletedCompany);

        boolean exists = companyRepository.existsByNameAndAddressAndDeletedAtIsNull("배송센터A", "서울시 강남구 테헤란로 1");

        assertThat(exists).isFalse();
    }
}
