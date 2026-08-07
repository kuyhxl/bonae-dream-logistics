package com.bonae.logistics.common.response;

import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestDtoTest {

    private PageRequestDto dto(Integer page, Integer size, String sort, String direction) {
        PageRequestDto dto = new PageRequestDto();
        dto.setPage(page);
        dto.setSize(size);
        dto.setSort(sort);
        dto.setDirection(direction);
        return dto;
    }

    @Nested
    @DisplayName("page")
    class Page {

        @Test
        @DisplayName("1-based 요청이 0-based로 변환된다")
        void convertsToZeroBased() {
            Pageable pageable = dto(1, 10, "createdAt", "desc").toPageable();

            assertThat(pageable.getPageNumber()).isZero();
        }

        @Test
        @DisplayName("2페이지 요청 시 offset이 size만큼 이동한다")
        void secondPageHasOffset() {
            Pageable pageable = dto(2, 10, "createdAt", "desc").toPageable();

            assertThat(pageable.getPageNumber()).isEqualTo(1);
            assertThat(pageable.getOffset()).isEqualTo(10);
        }

        @Test
        @DisplayName("생략하면 1페이지로 보정된다")
        void nullFallsBackToFirstPage() {
            Pageable pageable = dto(null, 10, "createdAt", "desc").toPageable();

            assertThat(pageable.getPageNumber()).isZero();
        }

        @Test
        @DisplayName("0 이하이면 1페이지로 보정된다")
        void nonPositiveFallsBackToFirstPage() {
            Pageable pageable = dto(0, 10, "createdAt", "desc").toPageable();

            assertThat(pageable.getPageNumber()).isZero();
        }
    }

    @Nested
    @DisplayName("size")
    class Size {

        @Test
        @DisplayName("허용된 크기는 그대로 적용된다")
        void allowedSizeIsKept() {
            Pageable pageable = dto(1, 30, "createdAt", "desc").toPageable();

            assertThat(pageable.getPageSize()).isEqualTo(30);
        }

        @Test
        @DisplayName("허용되지 않은 크기는 기본값 10으로 보정된다")
        void disallowedSizeFallsBackToDefault() {
            Pageable pageable = dto(1, 20, "createdAt", "desc").toPageable();

            assertThat(pageable.getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("생략하면 기본값 10이 적용된다")
        void nullFallsBackToDefault() {
            Pageable pageable = dto(1, null, "createdAt", "desc").toPageable();

            assertThat(pageable.getPageSize()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("sort")
    class SortField {

        @Test
        @DisplayName("허용된 정렬 기준은 그대로 적용된다")
        void allowedSortIsKept() {
            Pageable pageable = dto(1, 10, "updatedAt", "desc").toPageable();

            assertThat(pageable.getSort().getOrderFor("updatedAt")).isNotNull();
        }

        @Test
        @DisplayName("생략하면 createdAt이 적용된다")
        void blankFallsBackToDefault() {
            Pageable pageable = dto(1, 10, "  ", "desc").toPageable();

            assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
        }

        @Test
        @DisplayName("허용되지 않은 정렬 기준은 예외가 발생한다")
        void disallowedSortThrows() {
            PageRequestDto dto = dto(1, 10, "name", "desc");

            assertThatThrownBy(dto::toPageable)
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_SORT_FIELD);
        }
    }

    @Nested
    @DisplayName("direction")
    class Direction {

        @Test
        @DisplayName("asc는 오름차순으로 적용된다")
        void ascApplied() {
            Pageable pageable = dto(1, 10, "createdAt", "asc").toPageable();

            assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                    .isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("대소문자를 구분하지 않는다")
        void ignoresCase() {
            Pageable pageable = dto(1, 10, "createdAt", "ASC").toPageable();

            assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                    .isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("생략하면 내림차순이 적용된다")
        void blankFallsBackToDesc() {
            Pageable pageable = dto(1, 10, "createdAt", null).toPageable();

            assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("허용되지 않은 값은 내림차순으로 보정된다")
        void disallowedFallsBackToDesc() {
            Pageable pageable = dto(1, 10, "createdAt", "오타").toPageable();

            assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
        }
    }
}