package com.bonae.logistics.common.response;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

@Getter
@Setter
public class PageRequestDto {

    private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);
    private static final Set<String> ALLOWED_SORTS = Set.of("createdAt", "updatedAt");

    private int page = 1;
    private int size = 10;
    private String sort = "createdAt";
    private String direction = "desc";

    public Pageable toPageable() {
        validate();
        Sort.Direction dir = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // 1-based -> 0-based 변환
        return PageRequest.of(page - 1, size, Sort.by(dir, sort));
    }

    private void validate() {
        if (page < 1) {
            page = 1;
        }
        if (!ALLOWED_SIZES.contains(size)) {
            size = 10; // 수정!
        }
        if (!ALLOWED_SORTS.contains(sort)) {
            throw new BusinessException(ErrorCode.INVALID_SORT_FIELD);
        }
    }
}