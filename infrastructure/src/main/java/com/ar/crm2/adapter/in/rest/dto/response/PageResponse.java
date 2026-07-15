package com.ar.crm2.adapter.in.rest.dto.response;

import com.ar.crm2.application.shared.query.PagedResult;

import java.util.List;
import java.util.function.Function;

/** REST DTO for paginated list responses. */
public record PageResponse<T>(
        List<T> items,
        long totalItems,
        int page,
        int pageSize,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public static <D, R> PageResponse<R> from(PagedResult<D> result, Function<D, R> mapper) {
        return new PageResponse<>(
            result.items().stream().map(mapper).toList(),
            result.totalItems(),
            result.page(),
            result.pageSize(),
            result.totalPages(),
            result.hasNext(),
            result.hasPrevious()
        );
    }
}
