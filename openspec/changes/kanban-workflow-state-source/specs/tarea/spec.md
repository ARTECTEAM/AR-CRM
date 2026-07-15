# Delta Specification: Tarea Operational Status

## ADDED Requirements

### Requirement: Tarea operational status derives from Kanban

The system MUST NOT add `estado` to `Tarea` as a duplicated source of truth while Kanban columns are the operational workflow source.

#### Scenario: Tarea has a Kanban ficha

- GIVEN a `Tarea` exists
- AND a `Ficha` with `tipoFicha = TAREA` references that tarea
- WHEN a client needs to display the tarea operational status
- THEN the client MUST resolve it from the ficha's current column in a `TAREAS` tablero
- AND the `Tarea` response MUST remain free of an `estado` field

#### Scenario: Tarea has no Kanban ficha

- GIVEN a `Tarea` exists without a matching `TAREA` ficha
- WHEN a client needs to display operational status
- THEN the client SHOULD display a safe fallback such as `Sin columna`
- AND the backend MUST NOT synthesize a separate task status to hide the missing ficha
