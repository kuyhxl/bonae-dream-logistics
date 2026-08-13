package com.bonae.logistics.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Getter
@Builder
public class PageResponseDto<T> {

    private final List<T> content;

    @Schema(description = "현재 페이지 번호 (1-based)", example = "1")
    private final int page;

    @Schema(description = "페이지 크기", example = "10")
    private final int size;

    @Schema(description = "전체 항목 수", example = "42")
    private final long totalElements;

    @Schema(description = "전체 페이지 수", example = "5")
    private final int totalPages;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private final boolean last;

    public static <T> PageResponseDto<T> from(Page<T> page) {
        return PageResponseDto.<T>builder()
                .content(page.getContent())
                .page(page.getNumber() + 1)   // 0-based --> 1-based
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    // 엔티티 Page를 응답 Dto로 변환할 때 사용
    public static <E, T> PageResponseDto<T> from(Page<E> page, Function<E, T> mapper) {
        return PageResponseDto.<T>builder()
                .content(page.getContent().stream().map(mapper).toList())
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}