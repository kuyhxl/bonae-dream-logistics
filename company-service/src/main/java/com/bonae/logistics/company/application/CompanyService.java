package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.company.domain.Company;
import com.bonae.logistics.company.infrastructure.CompanyRepository;
import com.bonae.logistics.company.presentation.ReqCreateCompanyDto;
import com.bonae.logistics.company.presentation.ResCreateCompanyDto;
import lombok.RequiredArgsConstructor;
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

        Company savedCompany = companyRepository.save(company);
        return ResCreateCompanyDto.from(savedCompany);
    }
}
