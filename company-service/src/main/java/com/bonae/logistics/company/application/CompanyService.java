package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.auth.UserRole;
import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.bonae.logistics.company.domain.repository.CompanyRepository;
import com.bonae.logistics.company.infrastructure.HubClient;
import com.bonae.logistics.company.infrastructure.UserClient;
import com.bonae.logistics.company.infrastructure.UserInfoDto;
import com.bonae.logistics.company.presentation.dto.request.ReqCreateCompanyDto;
import com.bonae.logistics.company.presentation.dto.request.ReqUpdateCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResCreateCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyInternalDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyListDto;
import com.bonae.logistics.company.presentation.dto.response.ResSearchCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResUpdateCompanyDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    // ux_p_companies_name_address_active: (name, address) where deleted_at is null 부분 유니크 인덱스
    private static final String COMPANY_NAME_ADDRESS_UNIQUE_CONSTRAINT = "ux_p_companies_name_address_active";
    private static final String COMPANY_TYPE_ALL = "ALL";

    private final CompanyRepository companyRepository;
    private final HubClient hubClient;
    private final UserClient userClient;
    private final TransactionTemplate transactionTemplate;

    //@Transactional 제거
    public ResCreateCompanyDto createCompany(ReqCreateCompanyDto reqDto) {
        // hub-service 호출은 외부 API 응답 지연이 DB 트랜잭션(커넥션 점유)을 붙잡지 않도록 트랜잭션 밖에서 수행한다.
        validateHubExists(reqDto.getHubId());

        // 실제 DB 작업만 트랜잭션으로 처리
        Company company = transactionTemplate.execute(status -> {

            //삭제되지 않은 업체 중 동일 업체명+주소가 있는지 검증
            if (companyRepository.existsByNameAndAddressAndDeletedAtIsNull(reqDto.getName(), reqDto.getAddress())) {
                throw new BusinessException(ErrorCode.COMPANY_DUPLICATED);
            }

            Company newCompany = new Company(reqDto.getName(), reqDto.getType(), reqDto.getHubId(), reqDto.getAddress());

            // 최종 방어선은 DB 부분 유니크 인덱스(name, address where deleted_at is null)이며,
            // 위반 시 saveAndFlush에서 예외가 발생하므로 중복 에러로 변환한다.
            // 그 외의 제약조건 위반은 예상치 못한 오류이므로 그대로 던져 공통 예외 처리기가 처리하도록 한다.
            try {
                companyRepository.saveAndFlush(newCompany);
            } catch (DataIntegrityViolationException e) {
                if (isCompanyNameAddressUniqueViolation(e)) {
                    throw new BusinessException(ErrorCode.COMPANY_DUPLICATED);
                }
                throw e;
            }

            return newCompany;
        });

        return ResCreateCompanyDto.from(company);
    }

    //@Transactional 제거 (createCompany와 동일하게 외부 서비스 호출을 트랜잭션 밖에서 수행)
    public ResUpdateCompanyDto updateCompany(UUID companyId, ReqUpdateCompanyDto reqDto,
                                             UserRole userRole, String username) {
        // 존재 여부(삭제 여부 포함)와 접근권한을 먼저 확인.
        // 존재하지도, 권한도 없는 요청 때문에
        // 아래 외부 서비스 호출(authorizeUpdate/validateHubExists)이 낭비되지 않도록 여기서 먼저 걸러낸다.
        Company target = companyRepository.findByIdAndDeletedAtIsNull(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        authorizeUpdate(target, userRole, username);

        // hub-service 호출은 외부 API 응답 지연이 DB 트랜잭션(커넥션 점유)을 붙잡지 않도록 트랜잭션 밖에서 수행
        if (reqDto.getHubId() != null) {
            validateHubExists(reqDto.getHubId());
        }

        // 실제 DB 작업만 트랜잭션으로 처리
        Company company = transactionTemplate.execute(status -> {
            // 외부 서비스 호출 중 업체가 삭제될 수 있으므로 실제 수정 직전에 재조회.
            // 최신 상태의 managed 엔티티를 수정해 삭제된 업체의 재생성을 방지하고,
            // JPA dirty checking으로 변경 사항을 반영한다.
            Company managedCompany = companyRepository.findByIdAndDeletedAtIsNull(companyId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

            // 이번 수정으로 반영될 이름/주소(요청에 없으면 기존 값 유지)로 중복 여부를 검증
            String effectiveName = reqDto.getName() != null ? reqDto.getName() : managedCompany.getName();
            String effectiveAddress = reqDto.getAddress() != null ? reqDto.getAddress() : managedCompany.getAddress();

            if (companyRepository.existsByNameAndAddressAndDeletedAtIsNullAndIdNot(effectiveName, effectiveAddress, companyId)) {
                throw new BusinessException(ErrorCode.COMPANY_DUPLICATED);
            }

            // 최종 방어선은 DB 부분 유니크 인덱스(name, address where deleted_at is null)이며,
            // 위반 시 saveAndFlush에서 예외가 발생하므로 중복 에러로 변환한다.
            try {
                managedCompany.update(reqDto.getName(), reqDto.getType(), reqDto.getHubId(), reqDto.getAddress());
                companyRepository.saveAndFlush(managedCompany);
            } catch (DataIntegrityViolationException e) {
                if (isCompanyNameAddressUniqueViolation(e)) {
                    throw new BusinessException(ErrorCode.COMPANY_DUPLICATED);
                }
                throw e;
            }

            return managedCompany;
        });

        return ResUpdateCompanyDto.from(company);
    }

    //@Transactional 제거 (외부 서비스 호출을 트랜잭션 밖에서 수행)
    public void deleteCompany(UUID companyId, UserRole userRole, String username) {
        // 존재 여부(삭제 여부 포함)와 접근권한을 먼저 확인.
        Company target = companyRepository.findByIdAndDeletedAtIsNull(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        authorizeDelete(target, userRole, username);

        // 실제 DB 작업만 트랜잭션으로 처리
        transactionTemplate.executeWithoutResult(status -> {
            // authorizeDelete(user-service 호출)가 끝날 때까지 시간이 걸리는 동안 다른 요청이
            // 이 업체를 먼저 삭제했을 수 있으므로, 실제 삭제 직전에 managed 엔티티를 다시 조회해 재검증
            Company managedCompany = companyRepository.findByIdAndDeletedAtIsNull(companyId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

            managedCompany.delete(username);
        });
    }

    @Transactional(readOnly = true)
    //삭제되지 않은 업체를 페이징 조회한다. type이 ALL이면 전체, 아니면 해당 유형만 조회한다.
    public PageResponseDto<ResGetCompanyListDto> getCompanies(PageRequestDto pageRequestDto, String type) {
        CompanyType companyType = parseCompanyType(type);
        Page<Company> companies = companyRepository.findAllByTypeAndDeletedAtIsNull(companyType, pageRequestDto.toPageable());
        return PageResponseDto.from(companies, ResGetCompanyListDto::from);
    }

    //@Transactional 제거 (hub-service 호출을 DB 조회와 분리하기 위해 조회 전용 메서드지만 readOnly 트랜잭션을 걸지 않는다)
    //업체명 키워드/타입/허브로 검색한다. 세 조건 모두 선택값이며, 삭제된 업체는 결과에서 제외한다.
    public PageResponseDto<ResSearchCompanyDto> searchCompanies(PageRequestDto pageRequestDto, String keyword,
                                                                 String type, UUID hubId) {
        // hubId가 있으면 존재하는(삭제되지 않은) 허브인지 먼저 확인
        if (hubId != null) {
            validateHubExists(hubId);
        }

        CompanyType companyType = parseCompanyType(type);
        String namePattern = StringUtils.hasText(keyword) ? "%" + keyword.trim() + "%" : null;
        Page<Company> companies = companyRepository.searchByKeywordAndTypeAndHubIdAndDeletedAtIsNull(
                namePattern, companyType, hubId, pageRequestDto.toPageable());
        return PageResponseDto.from(companies, ResSearchCompanyDto::from);
    }

    @Transactional(readOnly = true)
    //삭제되지 않은 업체를 단건 조회한다. 없거나 삭제된 업체는 COMPANY_NOT_FOUND로 응답한다.
    public ResGetCompanyDto getCompany(UUID companyId) {
        Company company = companyRepository.findByIdAndDeletedAtIsNull(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
        return ResGetCompanyDto.from(company);
    }

    @Transactional(readOnly = true)
    //다른 서비스 내부 호출용 업체 단건 조회. 삭제된 업체는 없는 업체와 동일하게 COMPANY_NOT_FOUND로 응답한다.
    public ResGetCompanyInternalDto getCompanyInternal(UUID companyId) {
        Company company = companyRepository.findByIdAndDeletedAtIsNull(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
        return ResGetCompanyInternalDto.from(company);
    }

    //업체 타입 변환 메서드(String -> CompanyType)
    private CompanyType parseCompanyType(String type) {
        if (type == null || COMPANY_TYPE_ALL.equalsIgnoreCase(type)) {
            return null;
        }
        try {
            return CompanyType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_COMPANY_TYPE);
        }
    }

    // HUB_MANAGER는 담당 허브 소속 업체만, COMPANY_MANAGER는 본인 업체만 수정할 수 있다. MASTER는 제한 없음.
    // 요청 헤더로는 X-User-Role, X-User-Id만 전달되므로, 소속 hubId/companyId는 user-service에 조회한다.
    private void authorizeUpdate(Company company, UserRole userRole, String username) {
        if (userRole != UserRole.HUB_MANAGER && userRole != UserRole.COMPANY_MANAGER) {
            return;
        }

        if (userRole == UserRole.HUB_MANAGER) {
            requireOwnHub(company, username);
        } else {
            UserInfoDto userInfo = getUserInfo(username);
            if (userInfo.companyId() == null || !userInfo.companyId().equals(company.getId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }
    }

    // HUB_MANAGER는 담당 허브 소속 업체만 삭제할 수 있다. MASTER는 제한 없음.
    private void authorizeDelete(Company company, UserRole userRole, String username) {
        if (userRole != UserRole.HUB_MANAGER) {
            return;
        }

        requireOwnHub(company, username);
    }

    // 요청자(username)의 소속 허브가 대상 업체의 허브와 같은지 확인한다. HUB_MANAGER 권한 검증에 공통으로 쓰인다.
    private void requireOwnHub(Company company, String username) {
        UserInfoDto userInfo = getUserInfo(username);
        if (userInfo.hubId() == null || !userInfo.hubId().equals(company.getHubId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    // user-service의 내부 API로 사용자의 소속 정보를 조회한다.
    private UserInfoDto getUserInfo(String username) {
        try {
            return userClient.getUserInfo(username);
        } catch (FeignException.NotFound e) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    //ux_p_companies_name_address_active 부분 유니크 인덱스 제약조건 위반 여부 확인 메서드
    private boolean isCompanyNameAddressUniqueViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException cve
                && COMPANY_NAME_ADDRESS_UNIQUE_CONSTRAINT.equalsIgnoreCase(cve.getConstraintName());
    }

    // hub-service의 내부 API로 허브 존재 여부(삭제되지 않고 존재)를 확인한다. 200이면 존재, 404면 미존재.
    private void validateHubExists(UUID hubId) {
        try {
            hubClient.getHub(hubId);
        } catch (FeignException.NotFound e) {
            throw new BusinessException(ErrorCode.HUB_NOT_FOUND);
        }
    }
}
