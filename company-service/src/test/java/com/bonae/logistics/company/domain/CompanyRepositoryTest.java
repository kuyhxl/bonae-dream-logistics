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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull_삭제되지않은업체를_정상조회됨")
    void findByIdAndDeletedAtIsNull_삭제되지않은업체를_정상조회됨() {
        Company savedCompany = companyRepository.save(
                new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1")
        );

        var foundCompany = companyRepository.findByIdAndDeletedAtIsNull(savedCompany.getId());

        assertThat(foundCompany).isPresent();
        assertThat(foundCompany.get().getName()).isEqualTo("배송센터A");
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull_삭제된업체는_빈값반환")
    void findByIdAndDeletedAtIsNull_삭제된업체는_빈값반환() {
        Company deletedCompany = new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        deletedCompany.delete("tester");
        Company savedCompany = companyRepository.save(deletedCompany);

        var foundCompany = companyRepository.findByIdAndDeletedAtIsNull(savedCompany.getId());

        assertThat(foundCompany).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull_존재하지않는id일때_빈값반환")
    void findByIdAndDeletedAtIsNull_존재하지않는id일때_빈값반환() {
        var foundCompany = companyRepository.findByIdAndDeletedAtIsNull(UUID.randomUUID());

        assertThat(foundCompany).isEmpty();
    }

    @Test
    @DisplayName("existsByNameAndAddressAndDeletedAtIsNullAndIdNot_자기자신만같은이름과주소를가질때_false반환")
    void existsByNameAndAddressAndDeletedAtIsNullAndIdNot_자기자신만같은이름과주소를가질때_false반환() {
        Company savedCompany = companyRepository.save(
                new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1")
        );

        boolean exists = companyRepository.existsByNameAndAddressAndDeletedAtIsNullAndIdNot(
                "배송센터A", "서울시 강남구 테헤란로 1", savedCompany.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByNameAndAddressAndDeletedAtIsNullAndIdNot_다른업체가같은이름과주소를가질때_true반환")
    void existsByNameAndAddressAndDeletedAtIsNullAndIdNot_다른업체가같은이름과주소를가질때_true반환() {
        companyRepository.save(new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1"));
        Company anotherCompany = companyRepository.save(
                new Company("배송센터B", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 송파구 올림픽로 1")
        );

        boolean exists = companyRepository.existsByNameAndAddressAndDeletedAtIsNullAndIdNot(
                "배송센터A", "서울시 강남구 테헤란로 1", anotherCompany.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("update_필드변경후flush하면_updatedAt과updatedBy가JPAAuditing으로자동갱신된다")
    void update_필드변경후flush하면_updatedAt과updatedBy가JPAAuditing으로자동갱신된다() throws InterruptedException {
        Company company = companyRepository.saveAndFlush(
                new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1")
        );
        var createdUpdatedAt = company.getUpdatedAt();

        // LocalDateTime의 해상도 차이로 값이 같아 보이는 걸 방지하기 위해 약간의 시간차를 둔다.
        Thread.sleep(10);
        company.update("배송센터A-수정", null, null, null);
        companyRepository.saveAndFlush(company);

        assertThat(company.getName()).isEqualTo("배송센터A-수정");
        assertThat(company.getUpdatedAt()).isAfter(createdUpdatedAt);
        assertThat(company.getUpdatedBy()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("delete_호출후flush하면_deletedAt과deletedBy가저장된다")
    void delete_호출후flush하면_deletedAt과deletedBy가저장된다() {
        Company company = companyRepository.saveAndFlush(
                new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1")
        );

        company.delete("tester");
        companyRepository.saveAndFlush(company);

        Company reloaded = companyRepository.findById(company.getId()).orElseThrow();
        assertThat(reloaded.getDeletedAt()).isNotNull();
        assertThat(reloaded.getDeletedBy()).isEqualTo("tester");
        assertThat(reloaded.isDeleted()).isTrue();
    }

    // 아래 검색 테스트들은 이 테스트가 만든 데이터끼리만 매칭되도록 각 테스트마다 새로 발급한 hubId로 결과 범위를 좁힌다.
    // 실제 개발 DB(@AutoConfigureTestDatabase Replace.NONE)를 공유하는 환경이라
    // 다른 세션/수동 테스트로 남아있을 수 있는 기존 데이터와 키워드/타입이 우연히 겹쳐도 영향받지 않게 하기 위함이다.

    @Test
    @DisplayName("searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull_키워드가이름에포함되면_조회된다")
    void searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull_키워드가이름에포함되면_조회된다() {
        UUID hubId = UUID.randomUUID();
        companyRepository.save(new Company("삼성전자", CompanyType.PRODUCER, hubId, "경기도 수원시 영통구"));
        companyRepository.save(new Company("LG전자", CompanyType.PRODUCER, hubId, "서울시 강서구"));
        Pageable pageable = PageRequest.of(0, 10);

        Page<Company> result = companyRepository.searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull(
                "%삼성%", null, hubId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("삼성전자");
    }

    @Test
    @DisplayName("searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull_타입으로필터링된다")
    void searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull_타입으로필터링된다() {
        UUID hubId = UUID.randomUUID();
        companyRepository.save(new Company("삼성전자", CompanyType.PRODUCER, hubId, "경기도 수원시 영통구"));
        companyRepository.save(new Company("쿠팡물류센터", CompanyType.RECEIVER, hubId, "서울시 강서구"));
        Pageable pageable = PageRequest.of(0, 10);

        Page<Company> result = companyRepository.searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull(
                null, CompanyType.RECEIVER, hubId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("쿠팡물류센터");
    }

    @Test
    @DisplayName("searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull_허브로필터링된다")
    void searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull_허브로필터링된다() {
        UUID targetHubId = UUID.randomUUID();
        companyRepository.save(new Company("삼성전자", CompanyType.PRODUCER, targetHubId, "경기도 수원시 영통구"));
        companyRepository.save(new Company("LG전자", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강서구"));
        Pageable pageable = PageRequest.of(0, 10);

        Page<Company> result = companyRepository.searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull(
                null, null, targetHubId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("삼성전자");
    }

    @Test
    @DisplayName("searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull_삭제된업체는_결과에서제외된다")
    void searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull_삭제된업체는_결과에서제외된다() {
        UUID hubId = UUID.randomUUID();
        Company deletedCompany = new Company("삼성전자", CompanyType.PRODUCER, hubId, "경기도 수원시 영통구");
        deletedCompany.delete("tester");
        companyRepository.save(deletedCompany);
        Pageable pageable = PageRequest.of(0, 10);

        Page<Company> result = companyRepository.searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull(
                "%삼성%", null, hubId, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull_조건이모두없으면_삭제되지않은업체가결과에포함된다")
    void searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull_조건이모두없으면_삭제되지않은업체가결과에포함된다() {
        Company saved1 = companyRepository.save(
                new Company("삼성전자", CompanyType.PRODUCER, UUID.randomUUID(), "경기도 수원시 영통구"));
        Company saved2 = companyRepository.save(
                new Company("LG전자", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강서구"));
        // 조건 없이 전체 조회하므로 기존 데이터와 섞일 수 있어, 정확한 개수 대신 이 둘이 포함되는지만 확인한다.
        Pageable pageable = PageRequest.of(0, 100);

        Page<Company> result = companyRepository.searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull(
                null, null, null, pageable);

        assertThat(result.getContent()).extracting(Company::getId)
                .contains(saved1.getId(), saved2.getId());
    }
}
