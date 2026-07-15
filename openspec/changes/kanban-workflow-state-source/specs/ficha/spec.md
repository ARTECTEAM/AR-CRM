# Delta Specification: Ficha Workflow Movement

## ADDED Requirements

### Requirement: Ficha movement MUST preserve board-type compatibility

The system MUST reject moving a `Ficha` to a target column that belongs to a different board type than the ficha type.

#### Scenario: TAREA ficha moves to a TAREAS column

- GIVEN a `Ficha` with `tipoFicha = TAREA`
- AND a target column assigned to a `TAREAS` tablero
- WHEN the user sends `PUT /api/fichas/mover-columna?id={fichaId}` with that target `columnaId`
- THEN the system MUST persist the ficha with the new `columnaId`
- AND the system MUST NOT create or update a `Tarea.estado` field

#### Scenario: TAREA ficha tries to move to a TRATOS column

- GIVEN a `Ficha` with `tipoFicha = TAREA`
- AND a target column assigned only to a `TRATOS` tablero
- WHEN the user sends `PUT /api/fichas/mover-columna?id={fichaId}` with that target `columnaId`
- THEN the system MUST reject the movement
- AND the existing ficha `columnaId` MUST remain unchanged

#### Scenario: TRATO ficha moves to a TRATOS column

- GIVEN a `Ficha` with `tipoFicha = TRATO`
- AND a target column assigned to a `TRATOS` tablero
- WHEN the user sends `PUT /api/fichas/mover-columna?id={fichaId}` with that target `columnaId`
- THEN the system MUST persist the ficha with the new `columnaId`
- AND the system MUST NOT change `Trato.estado`

#### Scenario: TRATO ficha tries to move to a TAREAS column

- GIVEN a `Ficha` with `tipoFicha = TRATO`
- AND a target column assigned only to a `TAREAS` tablero
- WHEN the user sends `PUT /api/fichas/mover-columna?id={fichaId}` with that target `columnaId`
- THEN the system MUST reject the movement
- AND the existing ficha `columnaId` MUST remain unchanged

### Requirement: Kanban column position is workflow source

The system MUST treat `Ficha.columnaId` as the persisted source for task operational status and deal pipeline stage.

#### Scenario: Task workflow state is resolved from ficha column

- GIVEN a `Tarea` with an associated `Ficha` of type `TAREA`
- WHEN clients retrieve fichas and tableros
- THEN clients MUST be able to resolve the task operational status from `Ficha.columnaId` and the TAREAS tablero column metadata
- AND the API MUST NOT expose a separate `Tarea.estado` as the source of truth

#### Scenario: Deal pipeline stage is resolved from ficha column

- GIVEN a `Trato` with an associated `Ficha` of type `TRATO`
- WHEN clients retrieve fichas and tableros
- THEN clients MUST be able to resolve the deal pipeline stage from `Ficha.columnaId` and the TRATOS tablero column metadata
- AND `Trato.estado` MUST remain the commercial outcome only
