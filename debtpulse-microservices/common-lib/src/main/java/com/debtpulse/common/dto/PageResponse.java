package com.debtpulse.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Serialization-friendly wrapper around a Spring Data {@link Page}. Keeps the paginated
 * response shape identical across every service (and identical to the monolith).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
