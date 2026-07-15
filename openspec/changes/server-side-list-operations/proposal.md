# Change: server-side-list-operations

## Intent
Make CRM list screens scalable by moving pagination and ordering to the backend for Empresas, Contactos, Tratos, and Tareas, while preserving existing filter/search query parameters.

## Scope
- Add a paginated list contract with `page`, `pageSize`, `sortBy`, and `sortDirection`.
- Keep existing non-paginated `get-all` behavior backward compatible.
- Execute filtering/pagination/order in persistence adapters using Spring Data, not in frontend memory or post-fetch Java streams.
- Adapt frontend API types/hooks so screens can consume paginated responses without breaking existing array consumers.

## Out of Scope
- RBAC/permissions.
- Global `/api/search` endpoint.
- Reporting/timeline/automation.
