package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.domain.entity.Company;
import com.bonae.logistics.company.domain.entity.CompanyType;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.ProductRepository;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductListDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

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
}