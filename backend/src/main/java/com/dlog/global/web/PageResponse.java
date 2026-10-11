package com.dlog.global.web;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Spring Data Page 를 그대로 직렬화하지 않고 필요한 필드만 노출하는 페이지 응답.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
