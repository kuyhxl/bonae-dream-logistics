package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
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
import com.bonae.logistics.company.presentation.dto.response.ResCreateProductDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

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

    private Company companyWithId(String name, CompanyType type, UUID hubId, String address) {
        Company company = new Company(name, type, hubId, address);
        ReflectionTestUtils.setField(company, "id", UUID.randomUUID());
        return company;
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