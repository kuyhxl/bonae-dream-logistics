package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.company.domain.Company;
import com.bonae.logistics.company.infrastructure.CompanyRepository;
import com.bonae.logistics.company.presentation.ReqCreateCompanyDto;
import com.bonae.logistics.company.presentation.ResCreateCompanyDto;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional
    public ResCreateCompanyDto createCompany(ReqCreateCompanyDto reqDto) {
        //삭제되지 않은 업체 중 동일 업체명+주소가 있는지 검증
        if (companyRepository.existsByNameAndAddressAndDeletedAtIsNull(reqDto.getName(), reqDto.getAddress())) {
            throw new BusinessException(ErrorCode.COMPANY_DUPLICATED);
        }

        Company company = new Company(reqDto.getName(),reqDto.getType(),reqDto.getHubId(),reqDto.getAddress());

        // 최종 방어선은 DB 부분 유니크 인덱스(name, address where deleted_at is null)이며,
        // 위반 시 saveAndFlush에서 예외가 발생하므로 중복 에러로 변환한다.
        try {
            companyRepository.saveAndFlush(company);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.COMPANY_DUPLICATED);
        }

        return ResCreateCompanyDto.from(company);
    }
}
