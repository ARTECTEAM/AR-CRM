# Delta Specification: Tablero Workflow Columns

## MODIFIED Requirements

### Requirement: Default TRATOS columns represent pipeline stages

Default columns for `TRATOS` tableros MUST represent pipeline stages and MUST NOT use final commercial outcome labels as the primary workflow model.

#### Scenario: Creating a TRATOS tablero with default columns

- GIVEN an authenticated user creates a `TRATOS` tablero with default columns enabled
- WHEN the system initializes the tablero columns
- THEN the default columns SHOULD represent open pipeline stages such as `Nuevo`, `Calificado`, `Propuesta`, and `Negociación`
- AND the system MUST NOT treat these columns as the source of `Trato.estado`

### Requirement: Default TAREAS columns represent operational workflow

Default columns for `TAREAS` tableros MUST represent task operational workflow.

#### Scenario: Creating a TAREAS tablero with default columns

- GIVEN an authenticated user creates a `TAREAS` tablero with default columns enabled
- WHEN the system initializes the tablero columns
- THEN the default columns SHOULD represent task workflow stages such as `Pendiente`, `En Curso`, `Finalizada`, and `Cancelada`
- AND clients MAY display the current task status using the matching column name
