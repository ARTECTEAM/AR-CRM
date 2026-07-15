package com.ar.crm2.application.shared.query;

/**
 * Application-level list pagination and ordering request.
 * Infrastructure decides how to translate it to the concrete persistence API.
 */
public record ListPageRequest(
        Integer page,
        Integer pageSize,
        String sortBy,
        SortDirection sortDirection
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 100;

    public enum SortDirection { ASC, DESC }

    public static ListPageRequest unpaged() {
        return new ListPageRequest(null, null, null, null);
    }

    public boolean isPaged() {
        return page != null || pageSize != null;
    }

    public int normalizedPage() {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    public int normalizedPageSize() {
        if (pageSize == null) return DEFAULT_PAGE_SIZE;
        if (pageSize < 1) return DEFAULT_PAGE_SIZE;
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public SortDirection normalizedSortDirection() {
        return sortDirection == null ? SortDirection.DESC : sortDirection;
    }
}
