package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.ProductRepository;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductInternalDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

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
}