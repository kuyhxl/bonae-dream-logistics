package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.company.auth.UserRole;
import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.Inventory;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.CompanyRepository;
import com.bonae.logistics.company.domain.repository.ProductRepository;
import com.bonae.logistics.company.infrastructure.HubClient;
import com.bonae.logistics.company.infrastructure.UserClient;
import com.bonae.logistics.company.infrastructure.UserInfoDto;
import com.bonae.logistics.company.presentation.dto.request.ReqCreateProductDto;
import com.bonae.logistics.company.presentation.dto.response.ResCreateProductDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    // ux_p_products_name_company_active: (name, company_id) where deleted_at is null 부분 유니크 인덱스
    private static final String PRODUCT_NAME_COMPANY_UNIQUE_CONSTRAINT = "ux_p_products_name_company_active";
    private static final int MIN_QUANTITY = 0;

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final InventoryService inventoryService;
    private final UserClient userClient;
    private final HubClient hubClient;
    private final TransactionTemplate transactionTemplate;

    //@Transactional 제거 (외부 API 호출을 DB 트랜잭션 밖에서 수행하기 위해서)
    public ResCreateProductDto createProduct(ReqCreateProductDto reqDto, UserRole userRole, String username) {
        // 외부 호출 없이 값만으로 판단 가능하므로 가장 먼저 검증
        validateQuantity(reqDto.getQuantity());

        Company company = companyRepository.findByIdAndDeletedAtIsNull(reqDto.getCompanyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        // 존재하는 활성 허브인지는 상품 생성 시점에 확인
        validateHubExists(reqDto.getHubId());

        authorizeCreate(reqDto.getHubId(), company, userRole, username);

        // 실제 DB 작업만 트랜잭션으로 처리.
        // 상품 저장과 초기 재고 생성이 한 트랜잭션으로 묶여 재고 생성이 실패하면 상품 저장도 함께 롤백.
        ProductCreationResult result = transactionTemplate.execute(status -> {
            if (productRepository.existsByNameAndCompany_IdAndDeletedAtIsNull(reqDto.getName(), company.getId())) {
                throw new BusinessException(ErrorCode.PRODUCT_DUPLICATED);
            }

            Product newProduct = Product.create(reqDto.getName(), company, reqDto.getPrice());

            // 최종 방어선은 DB 부분 유니크 인덱스(name, company_id where deleted_at is null)이며,
            // 위반 시 saveAndFlush에서 예외가 발생하므로 중복 에러로 변환한다.
            try {
                productRepository.saveAndFlush(newProduct);
            } catch (DataIntegrityViolationException e) {
                if (isProductNameCompanyUniqueViolation(e)) {
                    throw new BusinessException(ErrorCode.PRODUCT_DUPLICATED);
                }
                throw e;
            }

            Inventory newInventory = inventoryService.createInventory(newProduct, reqDto.getHubId(), reqDto.getQuantity());

            return new ProductCreationResult(newProduct, newInventory);
        });

        return ResCreateProductDto.from(result.product(), result.inventory());
    }

    private record ProductCreationResult(Product product, Inventory inventory) {
    }

    // MASTER는 제한 없음.
    // HUB_MANAGER는 요청한 hubId(상품 보관 허브)가 본인 담당 허브일 때만,
    // COMPANY_MANAGER는 본인 업체 상품만 생성 가능.
    // 업체 소속 허브와 상품 보관 허브는 다를 수 있으므로 HUB_MANAGER 판단은 company.getHubId()가 아니라 요청의 hubId를 기준으로 함.
    private void authorizeCreate(UUID requestHubId, Company company, UserRole userRole, String username) {
        if (userRole != UserRole.HUB_MANAGER && userRole != UserRole.COMPANY_MANAGER) {
            return;
        }

        UserInfoDto userInfo = getUserInfo(username);
        if (userRole == UserRole.HUB_MANAGER) {
            if (userInfo.hubId() == null || !userInfo.hubId().equals(requestHubId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        } else {
            if (userInfo.companyId() == null || !userInfo.companyId().equals(company.getId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
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

    // hub-service의 내부 API로 허브 존재 여부(삭제되지 않고 존재)를 확인한다. 200이면 존재, 404면 미존재.
    private void validateHubExists(UUID hubId) {
        try {
            hubClient.getHub(hubId);
        } catch (FeignException.NotFound e) {
            throw new BusinessException(ErrorCode.HUB_NOT_FOUND);
        }
    }

    // 수량(quantity)이 올바른 범위인지 검사하는 검증 메서드.
    //  null이거나 0보다 작으면 잘못된 수량으로 처리
    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < MIN_QUANTITY) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
    }

    //ux_p_products_name_company_active 부분 유니크 인덱스 제약조건 위반 여부 확인 메서드
    private boolean isProductNameCompanyUniqueViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException cve
                && PRODUCT_NAME_COMPANY_UNIQUE_CONSTRAINT.equalsIgnoreCase(cve.getConstraintName());
    }
}