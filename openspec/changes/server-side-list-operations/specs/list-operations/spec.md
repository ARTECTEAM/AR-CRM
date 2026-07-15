# list-operations delta spec

## ADDED Requirements

### Requirement: Paginated list contract
List endpoints for Empresas, Contactos, Tratos, and Tareas MUST accept optional `page`, `pageSize`, `sortBy`, and `sortDirection` query params.

#### Scenario: paginated request returns page metadata
- **WHEN** a client sends `page` or `pageSize`
- **THEN** the response includes `items`, `totalItems`, `page`, `pageSize`, `totalPages`, `hasNext`, and `hasPrevious`.

#### Scenario: legacy request remains compatible
- **WHEN** a client omits `page` and `pageSize`
- **THEN** the endpoint returns the existing array response.

### Requirement: Database-side list operations
Filtering, searching, ordering, and pagination SHOULD happen through persistence queries/pageable infrastructure rather than filtering a complete table in frontend memory.
