package com.ar.crm2.application.shared.query;

import java.util.List;

/** Application-level paged result. Contains domain/application models, never REST DTOs. */
public record PagedResult<T>(
        List<T> items,
        long totalItems,
        int page,
        int pageSize,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
