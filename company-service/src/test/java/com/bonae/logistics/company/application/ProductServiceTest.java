package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.auth.UserRole;
import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.bonae.logistics.company.domain.entity.Inventory;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.CompanyRepository;
import com.bonae.logistics.company.domain.repository.ProductRepository;
import com.bonae.logistics.company.infrastructure.HubClient;
import com.bonae.logistics.company.infrastructure.UserClient;
import com.bonae.logistics.company.infrastructure.UserInfoDto;
import com.bonae.logistics.company.presentation.dto.request.ReqCreateProductDto;
import com.bonae.logistics.company.presentation.dto.request.ReqUpdateProductDto;
import com.bonae.logistics.company.presentation.dto.response.ResCreateProductDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductInternalDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductListDto;
import com.bonae.logistics.company.presentation.dto.response.ResSearchProductDto;
import com.bonae.logistics.company.presentation.dto.response.ResUpdateProductDto;
import feign.FeignException;
import feign.Request;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private UserClient userClient;

    @Mock
    private HubClient hubClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    private static final String USERNAME = "manager01";

    @InjectMocks
    private ProductService productService;

    // TransactionTemplate은 실제 트랜잭션 없이 콜백을 그대로 실행해 단위 테스트에서 위임되도록 스텁한다.
    @BeforeEach
    void setUpTransactionTemplate() {
        lenient().when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    @DisplayName("getProducts_정상요청시_삭제되지않은상품목록을조회한다")
    void getProducts_정상요청시_삭제되지않은상품목록을조회한다() {
        Company company = new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = Product.create("갤럭시 스마트폰", company, new BigDecimal("1200000.00"));
        PageRequestDto pageRequestDto = new PageRequestDto();
        Page<Product> productPage = new PageImpl<>(List.of(product), pageRequestDto.toPageable(), 1);

        when(productRepository.findAllByDeletedAtIsNull(any(Pageable.class))).thenReturn(productPage);

        PageResponseDto<ResGetProductListDto> resDto = productService.getProducts(pageRequestDto);

        assertThat(resDto.getContent()).hasSize(1);
        assertThat(resDto.getContent().get(0).getName()).isEqualTo(product.getName());
        assertThat(resDto.getContent().get(0).getCompanyId()).isEqualTo(company.getId());
        assertThat(resDto.getContent().get(0).getPrice()).isEqualByComparingTo(product.getPrice());
        assertThat(resDto.getTotalElements()).isEqualTo(1);
        verify(productRepository).findAllByDeletedAtIsNull(any(Pageable.class));
    }

    @Test
    @DisplayName("getProducts_상품이없을때_빈목록을반환한다")
    void getProducts_상품이없을때_빈목록을반환한다() {
        PageRequestDto pageRequestDto = new PageRequestDto();
        Page<Product> emptyPage = new PageImpl<>(List.of(), pageRequestDto.toPageable(), 0);

        when(productRepository.findAllByDeletedAtIsNull(any(Pageable.class))).thenReturn(emptyPage);

        PageResponseDto<ResGetProductListDto> resDto = productService.getProducts(pageRequestDto);

        assertThat(resDto.getContent()).isEmpty();
        assertThat(resDto.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("getProducts_정렬기준이허용되지않을때_예외발생")
    void getProducts_정렬기준이허용되지않을때_예외발생() {
        PageRequestDto pageRequestDto = new PageRequestDto();
        pageRequestDto.setSort("invalidField");

        assertThatThrownBy(() -> productService.getProducts(pageRequestDto))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SORT_FIELD);
    }

    @Test
    @DisplayName("searchProducts_조건이없을때_전체상품을조회한다")
    void searchProducts_조건이없을때_전체상품을조회한다() {
        Company company = new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = Product.create("갤럭시 스마트폰", company, new BigDecimal("1200000.00"));
        PageRequestDto pageRequestDto = new PageRequestDto();
        Page<Product> productPage = new PageImpl<>(List.of(product), pageRequestDto.toPageable(), 1);

        when(productRepository.searchByKeywordAndCompanyIdAndDeletedAtIsNull(
                isNull(), isNull(), any(Pageable.class))).thenReturn(productPage);

        PageResponseDto<ResSearchProductDto> resDto = productService.searchProducts(pageRequestDto, null, null);

        assertThat(resDto.getContent()).hasSize(1);
        assertThat(resDto.getContent().get(0).getName()).isEqualTo(product.getName());
        verify(productRepository).searchByKeywordAndCompanyIdAndDeletedAtIsNull(
                isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("searchProducts_키워드가있을때_앞뒤공백을제거하고LIKE패턴으로전달한다")
    void searchProducts_키워드가있을때_앞뒤공백을제거하고LIKE패턴으로전달한다() {
        PageRequestDto pageRequestDto = new PageRequestDto();
        Page<Product> emptyPage = new PageImpl<>(List.of(), pageRequestDto.toPageable(), 0);

        when(productRepository.searchByKeywordAndCompanyIdAndDeletedAtIsNull(
                eq("%갤럭시%"), isNull(), any(Pageable.class))).thenReturn(emptyPage);

        productService.searchProducts(pageRequestDto, "  갤럭시  ", null);

        verify(productRepository).searchByKeywordAndCompanyIdAndDeletedAtIsNull(
                eq("%갤럭시%"), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("searchProducts_키워드가공백뿐일때_조건없이전달한다")
    void searchProducts_키워드가공백뿐일때_조건없이전달한다() {
        PageRequestDto pageRequestDto = new PageRequestDto();
        Page<Product> emptyPage = new PageImpl<>(List.of(), pageRequestDto.toPageable(), 0);

        when(productRepository.searchByKeywordAndCompanyIdAndDeletedAtIsNull(
                isNull(), isNull(), any(Pageable.class))).thenReturn(emptyPage);

        productService.searchProducts(pageRequestDto, "   ", null);

        verify(productRepository).searchByKeywordAndCompanyIdAndDeletedAtIsNull(
                isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("searchProducts_업체로검색시_해당업체조건으로조회한다")
    void searchProducts_업체로검색시_해당업체조건으로조회한다() {
        PageRequestDto pageRequestDto = new PageRequestDto();
        UUID companyId = UUID.randomUUID();
        Page<Product> emptyPage = new PageImpl<>(List.of(), pageRequestDto.toPageable(), 0);

        when(productRepository.searchByKeywordAndCompanyIdAndDeletedAtIsNull(
                isNull(), eq(companyId), any(Pageable.class))).thenReturn(emptyPage);

        productService.searchProducts(pageRequestDto, null, companyId);

        verify(productRepository).searchByKeywordAndCompanyIdAndDeletedAtIsNull(
                isNull(), eq(companyId), any(Pageable.class));
    }

    @Test
    @DisplayName("searchProducts_정렬기준이허용되지않을때_예외발생")
    void searchProducts_정렬기준이허용되지않을때_예외발생() {
        PageRequestDto pageRequestDto = new PageRequestDto();
        pageRequestDto.setSort("invalidField");

        assertThatThrownBy(() -> productService.searchProducts(pageRequestDto, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SORT_FIELD);

        verify(productRepository, never())
                .searchByKeywordAndCompanyIdAndDeletedAtIsNull(any(), any(), any());
    }

    @Test
    @DisplayName("getProduct_삭제되지않은상품일때_상품정보를반환한다")
    void getProduct_삭제되지않은상품일때_상품정보를반환한다() {
        Company company = new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = Product.create("갤럭시 스마트폰", company, new BigDecimal("1200000.00"));

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));

        ResGetProductDto resDto = productService.getProduct(product.getId());

        assertThat(resDto.getProductId()).isEqualTo(product.getId());
        assertThat(resDto.getName()).isEqualTo(product.getName());
        assertThat(resDto.getCompanyId()).isEqualTo(company.getId());
        assertThat(resDto.getPrice()).isEqualByComparingTo(product.getPrice());
        assertThat(resDto.getCreatedAt()).isEqualTo(product.getCreatedAt());
        assertThat(resDto.getCreatedBy()).isEqualTo(product.getCreatedBy());
        assertThat(resDto.getUpdatedAt()).isEqualTo(product.getUpdatedAt());
        assertThat(resDto.getUpdatedBy()).isEqualTo(product.getUpdatedBy());
    }

    @Test
    @DisplayName("getProduct_존재하지않거나삭제된상품일때_예외발생")
    void getProduct_존재하지않거나삭제된상품일때_예외발생() {
        // 삭제된 상품은 리포지토리 쿼리 조건(deletedAt IS NULL)에서 이미 걸러지므로 미존재와 동일하게 빈 값이 반환된다.
        UUID productId = UUID.randomUUID();

        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(productId))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("getProductInternal_삭제되지않은상품일때_상품정보를반환한다")
    void getProductInternal_삭제되지않은상품일때_상품정보를반환한다() {
        Company company = new Company("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = Product.create("갤럭시 스마트폰", company, new BigDecimal("1200000.00"));

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));

        ResGetProductInternalDto resDto = productService.getProductInternal(product.getId());

        assertThat(resDto.getProductId()).isEqualTo(product.getId());
        assertThat(resDto.getName()).isEqualTo(product.getName());
        assertThat(resDto.getCompanyId()).isEqualTo(company.getId());
        assertThat(resDto.getPrice()).isEqualByComparingTo(product.getPrice());
        assertThat(resDto.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("getProductInternal_존재하지않거나삭제된상품일때_예외발생")
    void getProductInternal_존재하지않거나삭제된상품일때_예외발생() {
        // 삭제된 상품은 리포지토리 쿼리 조건(deletedAt IS NULL)에서 이미 걸러지므로 미존재와 동일하게 빈 값이 반환된다.
        UUID productId = UUID.randomUUID();

        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductInternal(productId))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("createProduct_MASTER가유효한요청으로생성할때_상품생성성공")
    void createProduct_MASTER가유효한요청으로생성할때_상품생성성공() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        UUID hubId = UUID.randomUUID();
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(company.getId())
                .price(new BigDecimal("1200000.00"))
                .hubId(hubId)
                .quantity(100)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(hubId)).thenReturn(ResponseEntity.ok().build());
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNull(reqDto.getName(), company.getId()))
                .thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryService.createInventory(any(Product.class), eq(hubId), eq(100)))
                .thenReturn(inventoryWith(company, hubId, 100));

        ResCreateProductDto resDto = productService.createProduct(reqDto, UserRole.MASTER, USERNAME);

        assertThat(resDto.getName()).isEqualTo(reqDto.getName());
        assertThat(resDto.getCompanyId()).isEqualTo(company.getId());
        assertThat(resDto.getPrice()).isEqualByComparingTo(reqDto.getPrice());
        assertThat(resDto.getHubId()).isEqualTo(hubId);
        assertThat(resDto.getQuantity()).isEqualTo(100);
        verify(productRepository).saveAndFlush(any(Product.class));
        verify(userClient, never()).getUserInfo(any());
    }

    @Test
    @DisplayName("createProduct_수량이음수일때_예외발생")
    void createProduct_수량이음수일때_예외발생() {
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(UUID.randomUUID())
                .price(new BigDecimal("1000.00"))
                .hubId(UUID.randomUUID())
                .quantity(-1)
                .build();

        assertThatThrownBy(() -> productService.createProduct(reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_QUANTITY);

        verify(companyRepository, never()).findByIdAndDeletedAtIsNull(any());
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("createProduct_수량이0일때_정상생성된다")
    void createProduct_수량이0일때_정상생성된다() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        UUID hubId = UUID.randomUUID();
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(company.getId())
                .price(new BigDecimal("1000.00"))
                .hubId(hubId)
                .quantity(0)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(hubId)).thenReturn(ResponseEntity.ok().build());
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNull(reqDto.getName(), company.getId()))
                .thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryService.createInventory(any(Product.class), eq(hubId), eq(0)))
                .thenReturn(inventoryWith(company, hubId, 0));

        ResCreateProductDto resDto = productService.createProduct(reqDto, UserRole.MASTER, USERNAME);

        assertThat(resDto.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("createProduct_존재하지않는업체일때_예외발생")
    void createProduct_존재하지않는업체일때_예외발생() {
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(UUID.randomUUID())
                .price(new BigDecimal("1000.00"))
                .hubId(UUID.randomUUID())
                .quantity(10)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(reqDto.getCompanyId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);

        verify(hubClient, never()).getHub(any());
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("createProduct_존재하지않는허브일때_예외발생")
    void createProduct_존재하지않는허브일때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        UUID hubId = UUID.randomUUID();
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(company.getId())
                .price(new BigDecimal("1000.00"))
                .hubId(hubId)
                .quantity(10)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(hubId)).thenThrow(hubNotFoundException(hubId));

        assertThatThrownBy(() -> productService.createProduct(reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.HUB_NOT_FOUND);

        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("createProduct_가격이음수일때_예외발생")
    void createProduct_가격이음수일때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        UUID hubId = UUID.randomUUID();
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(company.getId())
                .price(new BigDecimal("-1"))
                .hubId(hubId)
                .quantity(10)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(hubId)).thenReturn(ResponseEntity.ok().build());
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNull(reqDto.getName(), company.getId()))
                .thenReturn(false);

        assertThatThrownBy(() -> productService.createProduct(reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PRICE);

        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("createProduct_가격이허용범위를초과할때_예외발생")
    void createProduct_가격이허용범위를초과할때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        UUID hubId = UUID.randomUUID();
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(company.getId())
                .price(new BigDecimal("10000000000.00"))
                .hubId(hubId)
                .quantity(10)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(hubId)).thenReturn(ResponseEntity.ok().build());
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNull(reqDto.getName(), company.getId()))
                .thenReturn(false);

        assertThatThrownBy(() -> productService.createProduct(reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PRICE);

        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("createProduct_가격의소수자릿수가컬럼스케일을초과할때_예외발생")
    void createProduct_가격의소수자릿수가컬럼스케일을초과할때_예외발생() {
        // price 컬럼은 decimal(12,2)라 소수 셋째 자리부터는 저장 시 조용히 반올림되어
        // 응답값과 실제 저장값이 달라질 수 있으므로 요청 단계에서 막아야 한다.
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        UUID hubId = UUID.randomUUID();
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(company.getId())
                .price(new BigDecimal("1000.999"))
                .hubId(hubId)
                .quantity(10)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(hubId)).thenReturn(ResponseEntity.ok().build());
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNull(reqDto.getName(), company.getId()))
                .thenReturn(false);

        assertThatThrownBy(() -> productService.createProduct(reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PRICE);

        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("createProduct_이미존재하는상품명과업체조합일때_예외발생")
    void createProduct_이미존재하는상품명과업체조합일때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        UUID hubId = UUID.randomUUID();
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(company.getId())
                .price(new BigDecimal("1000.00"))
                .hubId(hubId)
                .quantity(10)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(hubId)).thenReturn(ResponseEntity.ok().build());
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNull(reqDto.getName(), company.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_DUPLICATED);

        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("createProduct_사전검증통과후저장시점에이름업체유니크제약조건위반이발생할때_예외발생")
    void createProduct_사전검증통과후저장시점에유니크제약조건위반이발생할때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        UUID hubId = UUID.randomUUID();
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(company.getId())
                .price(new BigDecimal("1000.00"))
                .hubId(hubId)
                .quantity(10)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(hubId)).thenReturn(ResponseEntity.ok().build());
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNull(reqDto.getName(), company.getId()))
                .thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class)))
                .thenThrow(duplicateKeyException("ux_p_products_name_company_active"));

        assertThatThrownBy(() -> productService.createProduct(reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_DUPLICATED);
    }

    @Test
    @DisplayName("createProduct_저장시점에이름업체제약조건이아닌다른제약조건위반이발생할때_원본예외그대로전파")
    void createProduct_다른제약조건위반이발생할때_원본예외그대로전파() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        UUID hubId = UUID.randomUUID();
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(company.getId())
                .price(new BigDecimal("1000.00"))
                .hubId(hubId)
                .quantity(10)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(hubId)).thenReturn(ResponseEntity.ok().build());
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNull(reqDto.getName(), company.getId()))
                .thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class)))
                .thenThrow(duplicateKeyException("pk_p_products"));

        assertThatThrownBy(() -> productService.createProduct(reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("createProduct_HUB_MANAGER가담당허브가아닌곳으로생성하려할때_예외발생")
    void createProduct_HUB_MANAGER가담당허브가아닌곳으로생성하려할때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        UUID requestHubId = UUID.randomUUID();
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(company.getId())
                .price(new BigDecimal("1000.00"))
                .hubId(requestHubId)
                .quantity(10)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(requestHubId)).thenReturn(ResponseEntity.ok().build());
        when(userClient.getUserInfo(USERNAME)).thenReturn(new UserInfoDto(UUID.randomUUID(), null));

        assertThatThrownBy(() -> productService.createProduct(reqDto, UserRole.HUB_MANAGER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("createProduct_HUB_MANAGER가담당허브로생성할때_업체소속허브와달라도정상생성된다")
    void createProduct_HUB_MANAGER가담당허브로생성할때_업체소속허브와달라도정상생성된다() {
        // 업체 소속 허브(company의 hubId)와 요청 hubId(상품 보관 허브)를 의도적으로 다르게 둔다.
        UUID companyHubId = UUID.randomUUID();
        UUID requestHubId = UUID.randomUUID();
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, companyHubId, "서울시 강남구 테헤란로 1");
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(company.getId())
                .price(new BigDecimal("1000.00"))
                .hubId(requestHubId)
                .quantity(10)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(requestHubId)).thenReturn(ResponseEntity.ok().build());
        // HUB_MANAGER 본인 담당 허브는 요청 hubId와 일치하지만, 업체 소속 허브(companyHubId)와는 다르다.
        when(userClient.getUserInfo(USERNAME)).thenReturn(new UserInfoDto(requestHubId, null));
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNull(reqDto.getName(), company.getId()))
                .thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryService.createInventory(any(Product.class), eq(requestHubId), eq(10)))
                .thenReturn(inventoryWith(company, requestHubId, 10));

        ResCreateProductDto resDto = productService.createProduct(reqDto, UserRole.HUB_MANAGER, USERNAME);

        assertThat(resDto.getHubId()).isEqualTo(requestHubId);
        verify(productRepository).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("createProduct_COMPANY_MANAGER가본인업체가아닐때_예외발생")
    void createProduct_COMPANY_MANAGER가본인업체가아닐때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        UUID hubId = UUID.randomUUID();
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(company.getId())
                .price(new BigDecimal("1000.00"))
                .hubId(hubId)
                .quantity(10)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(hubId)).thenReturn(ResponseEntity.ok().build());
        when(userClient.getUserInfo(USERNAME)).thenReturn(new UserInfoDto(null, UUID.randomUUID()));

        assertThatThrownBy(() -> productService.createProduct(reqDto, UserRole.COMPANY_MANAGER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("createProduct_COMPANY_MANAGER가본인업체일때_정상생성된다")
    void createProduct_COMPANY_MANAGER가본인업체일때_정상생성된다() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        UUID hubId = UUID.randomUUID();
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(company.getId())
                .price(new BigDecimal("1000.00"))
                .hubId(hubId)
                .quantity(10)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(hubId)).thenReturn(ResponseEntity.ok().build());
        when(userClient.getUserInfo(USERNAME)).thenReturn(new UserInfoDto(null, company.getId()));
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNull(reqDto.getName(), company.getId()))
                .thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryService.createInventory(any(Product.class), eq(hubId), eq(10)))
                .thenReturn(inventoryWith(company, hubId, 10));

        ResCreateProductDto resDto = productService.createProduct(reqDto, UserRole.COMPANY_MANAGER, USERNAME);

        assertThat(resDto.getCompanyId()).isEqualTo(company.getId());
        verify(productRepository).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("createProduct_소속조회대상사용자를찾을수없을때_예외발생")
    void createProduct_소속조회대상사용자를찾을수없을때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        UUID hubId = UUID.randomUUID();
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(company.getId())
                .price(new BigDecimal("1000.00"))
                .hubId(hubId)
                .quantity(10)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(hubId)).thenReturn(ResponseEntity.ok().build());
        when(userClient.getUserInfo(USERNAME)).thenThrow(userNotFoundException(USERNAME));

        assertThatThrownBy(() -> productService.createProduct(reqDto, UserRole.HUB_MANAGER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("createProduct_재고가중복될때_예외발생")
    void createProduct_재고가중복될때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        UUID hubId = UUID.randomUUID();
        ReqCreateProductDto reqDto = ReqCreateProductDto.builder()
                .name("갤럭시 스마트폰")
                .companyId(company.getId())
                .price(new BigDecimal("1000.00"))
                .hubId(hubId)
                .quantity(10)
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(hubClient.getHub(hubId)).thenReturn(ResponseEntity.ok().build());
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNull(reqDto.getName(), company.getId()))
                .thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryService.createInventory(any(Product.class), eq(hubId), eq(10)))
                .thenThrow(new BusinessException(ErrorCode.INVENTORY_DUPLICATED));

        assertThatThrownBy(() -> productService.createProduct(reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_DUPLICATED);
    }

    @Test
    @DisplayName("deleteProduct_존재하지않거나삭제된상품일때_예외발생")
    void deleteProduct_존재하지않거나삭제된상품일때_예외발생() {
        UUID productId = UUID.randomUUID();

        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(productId, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteProduct_MASTER는_소속조회없이제한없이삭제가능하다")
    void deleteProduct_MASTER는_소속조회없이제한없이삭제가능하다() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));

        productService.deleteProduct(product.getId(), UserRole.MASTER, USERNAME);

        assertThat(product.getDeletedAt()).isNotNull();
        assertThat(product.getDeletedBy()).isEqualTo(USERNAME);
        verify(userClient, never()).getUserInfo(any());
        verify(companyRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("deleteProduct_HUB_MANAGER가담당허브가아닌상품을삭제하려할때_예외발생")
    void deleteProduct_HUB_MANAGER가담당허브가아닌상품을삭제하려할때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(userClient.getUserInfo(USERNAME)).thenReturn(new UserInfoDto(UUID.randomUUID(), null));

        assertThatThrownBy(() -> productService.deleteProduct(product.getId(), UserRole.HUB_MANAGER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        assertThat(product.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("deleteProduct_HUB_MANAGER권한확인중업체를찾을수없을때_예외발생")
    void deleteProduct_HUB_MANAGER권한확인중업체를찾을수없을때_예외발생() {
        // 상품 생성 이후 소속 업체가 소프트 삭제된 극단적인 경우를 가정한다.
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(product.getId(), UserRole.HUB_MANAGER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);

        verify(userClient, never()).getUserInfo(any());
        assertThat(product.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("deleteProduct_HUB_MANAGER가담당허브상품을삭제할때_정상삭제된다")
    void deleteProduct_HUB_MANAGER가담당허브상품을삭제할때_정상삭제된다() {
        UUID hubId = UUID.randomUUID();
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, hubId, "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(userClient.getUserInfo(USERNAME)).thenReturn(new UserInfoDto(hubId, null));

        productService.deleteProduct(product.getId(), UserRole.HUB_MANAGER, USERNAME);

        assertThat(product.getDeletedAt()).isNotNull();
        assertThat(product.getDeletedBy()).isEqualTo(USERNAME);
    }

    @Test
    @DisplayName("deleteProduct_소속조회대상사용자를찾을수없을때_예외발생")
    void deleteProduct_소속조회대상사용자를찾을수없을때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(userClient.getUserInfo(USERNAME)).thenThrow(userNotFoundException(USERNAME));

        assertThatThrownBy(() -> productService.deleteProduct(product.getId(), UserRole.HUB_MANAGER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        assertThat(product.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("updateProduct_존재하지않거나삭제된상품일때_예외발생")
    void updateProduct_존재하지않거나삭제된상품일때_예외발생() {
        UUID productId = UUID.randomUUID();
        ReqUpdateProductDto reqDto = ReqUpdateProductDto.builder().name("갤럭시 스마트폰 Pro").build();

        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(productId, reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("updateProduct_수정필드가모두없을때_예외발생")
    void updateProduct_수정필드가모두없을때_예외발생() {
        ReqUpdateProductDto reqDto = ReqUpdateProductDto.builder().build();

        assertThatThrownBy(() -> productService.updateProduct(UUID.randomUUID(), reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(productRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("updateProduct_name이공백뿐일때_예외발생")
    void updateProduct_name이공백뿐일때_예외발생() {
        ReqUpdateProductDto reqDto = ReqUpdateProductDto.builder().name("   ").build();

        assertThatThrownBy(() -> productService.updateProduct(UUID.randomUUID(), reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(productRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("updateProduct_MASTER는_소속조회없이제한없이수정가능하다")
    void updateProduct_MASTER는_소속조회없이제한없이수정가능하다() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));
        ReqUpdateProductDto reqDto = ReqUpdateProductDto.builder().name("갤럭시 스마트폰 Pro").build();

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNullAndIdNot(any(), any(), eq(product.getId())))
                .thenReturn(false);

        ResUpdateProductDto resDto = productService.updateProduct(product.getId(), reqDto, UserRole.MASTER, USERNAME);

        assertThat(resDto.getProductId()).isEqualTo(product.getId());
        assertThat(resDto.getName()).isEqualTo("갤럭시 스마트폰 Pro");
        assertThat(resDto.getCompanyId()).isEqualTo(company.getId());
        verify(productRepository).saveAndFlush(product);
        verify(userClient, never()).getUserInfo(any());
        verify(companyRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("updateProduct_일부필드만요청에포함될때_포함되지않은필드는유지된다")
    void updateProduct_일부필드만요청에포함될때_포함되지않은필드는유지된다() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));
        ReqUpdateProductDto reqDto = ReqUpdateProductDto.builder().price(new BigDecimal("1200000.00")).build();

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNullAndIdNot(any(), any(), eq(product.getId())))
                .thenReturn(false);

        ResUpdateProductDto resDto = productService.updateProduct(product.getId(), reqDto, UserRole.MASTER, USERNAME);

        assertThat(resDto.getName()).isEqualTo("갤럭시 스마트폰");
        assertThat(resDto.getPrice()).isEqualByComparingTo("1200000.00");
    }

    @Test
    @DisplayName("updateProduct_HUB_MANAGER가담당허브가아닌업체상품을수정하려할때_예외발생")
    void updateProduct_HUB_MANAGER가담당허브가아닌업체상품을수정하려할때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));
        ReqUpdateProductDto reqDto = ReqUpdateProductDto.builder().name("갤럭시 스마트폰 Pro").build();

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(userClient.getUserInfo(USERNAME)).thenReturn(new UserInfoDto(UUID.randomUUID(), null));

        assertThatThrownBy(() -> productService.updateProduct(product.getId(), reqDto, UserRole.HUB_MANAGER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("updateProduct_HUB_MANAGER권한확인중업체를찾을수없을때_예외발생")
    void updateProduct_HUB_MANAGER권한확인중업체를찾을수없을때_예외발생() {
        // 상품 생성 이후 소속 업체가 소프트 삭제된 극단적인 경우를 가정한다.
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));
        ReqUpdateProductDto reqDto = ReqUpdateProductDto.builder().name("갤럭시 스마트폰 Pro").build();

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(product.getId(), reqDto, UserRole.HUB_MANAGER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);

        verify(userClient, never()).getUserInfo(any());
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("updateProduct_HUB_MANAGER가담당허브업체상품을수정할때_정상수정된다")
    void updateProduct_HUB_MANAGER가담당허브업체상품을수정할때_정상수정된다() {
        UUID hubId = UUID.randomUUID();
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, hubId, "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));
        ReqUpdateProductDto reqDto = ReqUpdateProductDto.builder().price(new BigDecimal("1200000.00")).build();

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(userClient.getUserInfo(USERNAME)).thenReturn(new UserInfoDto(hubId, null));
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNullAndIdNot(any(), any(), eq(product.getId())))
                .thenReturn(false);

        ResUpdateProductDto resDto =
                productService.updateProduct(product.getId(), reqDto, UserRole.HUB_MANAGER, USERNAME);

        assertThat(resDto.getPrice()).isEqualByComparingTo("1200000.00");
    }

    @Test
    @DisplayName("updateProduct_COMPANY_MANAGER가본인업체가아닌상품을수정하려할때_예외발생")
    void updateProduct_COMPANY_MANAGER가본인업체가아닌상품을수정하려할때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));
        ReqUpdateProductDto reqDto = ReqUpdateProductDto.builder().name("갤럭시 스마트폰 Pro").build();

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(userClient.getUserInfo(USERNAME)).thenReturn(new UserInfoDto(null, UUID.randomUUID()));

        assertThatThrownBy(() -> productService.updateProduct(product.getId(), reqDto, UserRole.COMPANY_MANAGER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(companyRepository, never()).findByIdAndDeletedAtIsNull(any());
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("updateProduct_COMPANY_MANAGER가본인업체상품을수정할때_정상수정된다")
    void updateProduct_COMPANY_MANAGER가본인업체상품을수정할때_정상수정된다() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));
        ReqUpdateProductDto reqDto = ReqUpdateProductDto.builder().name("갤럭시 스마트폰 Pro").build();

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(userClient.getUserInfo(USERNAME)).thenReturn(new UserInfoDto(null, company.getId()));
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNullAndIdNot(any(), any(), eq(product.getId())))
                .thenReturn(false);

        ResUpdateProductDto resDto =
                productService.updateProduct(product.getId(), reqDto, UserRole.COMPANY_MANAGER, USERNAME);

        assertThat(resDto.getName()).isEqualTo("갤럭시 스마트폰 Pro");
        verify(companyRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("updateProduct_소속조회대상사용자를찾을수없을때_예외발생")
    void updateProduct_소속조회대상사용자를찾을수없을때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));
        ReqUpdateProductDto reqDto = ReqUpdateProductDto.builder().name("갤럭시 스마트폰 Pro").build();

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(companyRepository.findByIdAndDeletedAtIsNull(company.getId())).thenReturn(Optional.of(company));
        when(userClient.getUserInfo(USERNAME)).thenThrow(userNotFoundException(USERNAME));

        assertThatThrownBy(() -> productService.updateProduct(product.getId(), reqDto, UserRole.HUB_MANAGER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("updateProduct_이름과업체가같은다른상품이있을때_예외발생")
    void updateProduct_이름과업체가같은다른상품이있을때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));
        ReqUpdateProductDto reqDto = ReqUpdateProductDto.builder().name("아이폰").build();

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNullAndIdNot(
                "아이폰", company.getId(), product.getId())).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(product.getId(), reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_DUPLICATED);

        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("updateProduct_가격이유효하지않을때_예외발생")
    void updateProduct_가격이유효하지않을때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));
        ReqUpdateProductDto reqDto = ReqUpdateProductDto.builder().price(new BigDecimal("-1")).build();

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNullAndIdNot(any(), any(), eq(product.getId())))
                .thenReturn(false);

        assertThatThrownBy(() -> productService.updateProduct(product.getId(), reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PRICE);

        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("updateProduct_저장시점에이름업체유니크제약조건위반이발생할때_예외발생")
    void updateProduct_저장시점에이름업체유니크제약조건위반이발생할때_예외발생() {
        Company company = companyWithId("배송센터A", CompanyType.PRODUCER, UUID.randomUUID(), "서울시 강남구 테헤란로 1");
        Product product = productWithId("갤럭시 스마트폰", company, new BigDecimal("1000000.00"));
        ReqUpdateProductDto reqDto = ReqUpdateProductDto.builder().name("갤럭시 스마트폰 Pro").build();

        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.existsByNameAndCompany_IdAndDeletedAtIsNullAndIdNot(any(), any(), eq(product.getId())))
                .thenReturn(false);
        doThrow(duplicateKeyException("ux_p_products_name_company_active"))
                .when(productRepository).saveAndFlush(any(Product.class));

        assertThatThrownBy(() -> productService.updateProduct(product.getId(), reqDto, UserRole.MASTER, USERNAME))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_DUPLICATED);
    }

    private Company companyWithId(String name, CompanyType type, UUID hubId, String address) {
        Company company = new Company(name, type, hubId, address);
        ReflectionTestUtils.setField(company, "id", UUID.randomUUID());
        return company;
    }

    private Product productWithId(String name, Company company, BigDecimal price) {
        Product product = Product.create(name, company, price);
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        return product;
    }

    // Inventory.create()는 product가 null이 아니기만 하면 되므로, 응답 매핑에 쓰이는 hubId/quantity만 의미 있게 채운다.
    private Inventory inventoryWith(Company company, UUID hubId, Integer quantity) {
        Product product = Product.create("더미상품", company, BigDecimal.ONE);
        return Inventory.create(product, hubId, quantity);
    }

    private DataIntegrityViolationException duplicateKeyException(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException(
                "could not execute statement",
                new SQLException("duplicate key value violates unique constraint \"" + constraintName + "\""),
                constraintName
        );
        return new DataIntegrityViolationException("duplicate key", cause);
    }

    private FeignException.NotFound hubNotFoundException(UUID hubId) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://hub-service/api/internal/hubs/" + hubId,
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null
        );
        return new FeignException.NotFound("hub not found", request, null, Collections.emptyMap());
    }

    private FeignException.NotFound userNotFoundException(String username) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://user-service/api/internal/users/" + username,
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null
        );
        return new FeignException.NotFound("user not found", request, null, Collections.emptyMap());
    }
}