package com.bonae.logistics.company.application;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.ProductRepository;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    //삭제되지 않은 상품을 단건 조회한다. 없거나 삭제된 상품은 PRODUCT_NOT_FOUND로 응답한다.
    public ResGetProductDto getProduct(UUID productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return ResGetProductDto.from(product);
    }
}