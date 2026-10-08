package com.ar.crm2.application.shared.query;

import java.util.List;

/** Creates a page from rows already filtered by authorization, so totals cannot reveal hidden records. */
public final class AuthorizedListPage {
    private AuthorizedListPage() {
    }

    public static <T> PagedResult<T> slice(List<T> authorizedItems, ListPageRequest request) {
        ListPageRequest resolved = request == null ? ListPageRequest.unpaged() : request;
        int page = resolved.normalizedPage();
        int pageSize = resolved.normalizedPageSize();
        long totalItems = authorizedItems.size();
        long firstIndex = (long) page * pageSize;
        int from = (int) Math.min(firstIndex, totalItems);
        int to = (int) Math.min(firstIndex + pageSize, totalItems);
        int totalPages = (int) ((totalItems + pageSize - 1L) / pageSize);
        return new PagedResult<>(authorizedItems.subList(from, to), totalItems, page, pageSize, totalPages,
                firstIndex + pageSize < totalItems, page > 0);
    }
}