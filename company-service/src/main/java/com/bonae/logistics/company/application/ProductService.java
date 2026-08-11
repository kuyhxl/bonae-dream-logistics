package com.bonae.logistics.company.application;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.domain.entity.Product;
import com.bonae.logistics.company.domain.repository.ProductRepository;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductListDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    //삭제되지 않은 상품을 페이징 조회한다.
    public PageResponseDto<ResGetProductListDto> getProducts(PageRequestDto pageRequestDto) {
        Page<Product> products = productRepository.findAllByDeletedAtIsNull(pageRequestDto.toPageable());
        return PageResponseDto.from(products, ResGetProductListDto::from);
    }
}