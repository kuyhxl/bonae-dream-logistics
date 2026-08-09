package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.bonae.logistics.company.domain.repository.CompanyRepository;
import com.bonae.logistics.company.infrastructure.HubClient;
import com.bonae.logistics.company.presentation.dto.request.ReqCreateCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResCreateCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyInternalDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyListDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    // ux_p_companies_name_address_active: (name, address) where deleted_at is null 부분 유니크 인덱스
    private static final String COMPANY_NAME_ADDRESS_UNIQUE_CONSTRAINT = "ux_p_companies_name_address_active";
    private static final String COMPANY_TYPE_ALL = "ALL";

    private final CompanyRepository companyRepository;
    private final HubClient hubClient;
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

    @Transactional(readOnly = true)
    //삭제되지 않은 업체를 페이징 조회한다. type이 ALL이면 전체, 아니면 해당 유형만 조회한다.
    public PageResponseDto<ResGetCompanyListDto> getCompanies(PageRequestDto pageRequestDto, String type) {
        CompanyType companyType = parseCompanyType(type);
        Page<Company> companies = companyRepository.findAllByTypeAndDeletedAtIsNull(companyType, pageRequestDto.toPageable());
        return PageResponseDto.from(companies, ResGetCompanyListDto::from);
    }

    @Transactional(readOnly = true)
    //다른 서비스 내부 호출용 업체 단건 조회. 삭제 여부와 무관하게 조회하고 isDeleted로 삭제 상태를 알려준다.
    public ResGetCompanyInternalDto getCompanyInternal(UUID companyId) {
        Company company = companyRepository.findById(companyId)
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
