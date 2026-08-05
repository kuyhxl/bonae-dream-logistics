package com.bonae.logistics.company.application;

import com.bonae.logistics.company.domain.Company;
import com.bonae.logistics.company.exception.CompanyErrorCode;
import com.bonae.logistics.company.exception.CompanyException;
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
            throw new CompanyException(CompanyErrorCode.COMPANY_DUPLICATED); //TODO : 임시로 CompanyException 으로 던짐
        }

        Company company = new Company(reqDto.getName(),reqDto.getType(),reqDto.getHubId(),reqDto.getAddress());

        Company savedCompany = companyRepository.save(company);
        return ResCreateCompanyDto.from(savedCompany);
    }
}
