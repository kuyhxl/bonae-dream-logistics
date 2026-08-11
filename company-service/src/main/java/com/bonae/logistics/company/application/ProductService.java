package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.ProductRepository;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductInternalDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    //다른 서비스 내부 호출용 상품 단건 조회. 삭제된 상품은 없는 상품과 동일하게 PRODUCT_NOT_FOUND로 응답한다.
    public ResGetProductInternalDto getProductInternal(UUID productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return ResGetProductInternalDto.from(product);
    }
}