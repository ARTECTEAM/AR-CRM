# Design: server-side-list-operations

## Backend
- Application owns query contracts: filters plus optional list page options.
- Infrastructure REST adapters parse query params and translate paginated responses to DTOs.
- Persistence adapters build JPA `Specification` + `Pageable`/`Sort` so filtering and paging happen in the database.
- Domain remains untouched; list mechanics are application/infrastructure concerns.

## Frontend
- API contract adds `PageResponse<T>` and list query options.
- Hooks keep existing array-returning behavior for compatibility and expose paginated variants for server-driven screens.

## Compatibility
- No `page`/`pageSize`: endpoints return `List<Response>` as today.
- With `page` or `pageSize`: endpoints return `{ items, totalItems, page, pageSize, totalPages, hasNext, hasPrevious }`.
