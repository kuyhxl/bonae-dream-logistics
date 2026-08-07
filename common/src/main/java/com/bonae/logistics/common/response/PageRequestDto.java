package com.bonae.logistics.common.response;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

@Getter
@Setter
public class PageRequestDto {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final String DEFAULT_SORT = "createdAt";
    private static final String DEFAULT_DIRECTION = "desc";

    private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);
    private static final Set<String> ALLOWED_SORTS = Set.of("createdAt", "updatedAt");
    private static final Set<String> ALLOWED_DIRECTIONS = Set.of("asc", "desc");

    // 쿼리 파라미터 생략과 빈 값을 구분하기 위해 래퍼로 변경
    private Integer page;
    private Integer size;
    private String sort;
    private String direction;

    public Pageable toPageable() {
        normalize();
        validate();

        Sort.Direction dir = Sort.Direction.fromString(direction);
        // 1-based -> 0-based 변환
        return PageRequest.of(page - 1, size, Sort.by(dir, sort));
    }

    private void normalize() {
        if (page == null || page < 1) {
            page = DEFAULT_PAGE;
        }
        if (size == null || !ALLOWED_SIZES.contains(size)) {
            size = DEFAULT_SIZE;
        }
        if (!StringUtils.hasText(sort)) {
            sort = DEFAULT_SORT;
        }
        // 이후 로직이 소문자를 전제하도록 여기서 정규화
        direction = StringUtils.hasText(direction) ? direction.toLowerCase(Locale.ROOT) : DEFAULT_DIRECTION;
        if (!ALLOWED_DIRECTIONS.contains(direction)) {
            direction = DEFAULT_DIRECTION;
        }
    }

    private void validate() {
        if (!ALLOWED_SORTS.contains(sort)) {
            throw new BusinessException(ErrorCode.INVALID_SORT_FIELD);
        }
    }
}