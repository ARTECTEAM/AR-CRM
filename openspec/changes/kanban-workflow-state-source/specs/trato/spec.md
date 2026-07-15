# Delta Specification: Trato Outcome vs Pipeline Stage

## MODIFIED Requirements

### Requirement: Trato estado remains commercial outcome

`Trato.estado` MUST represent only the commercial outcome of a deal: `ABIERTO`, `GANADO`, or `PERDIDO`.

#### Scenario: Moving a TRATO ficha across pipeline columns

- GIVEN a `Trato` with `estado = ABIERTO`
- AND a `Ficha` of type `TRATO` linked to that trato
- WHEN the ficha is moved to another TRATOS column through `PUT /api/fichas/mover-columna?id={fichaId}`
- THEN the ficha `columnaId` MUST change
- AND the `Trato.estado` MUST remain `ABIERTO`

#### Scenario: Winning a trato

- GIVEN a `Trato` with `estado = ABIERTO`
- WHEN the user sends `PUT /api/tratos/ganar?id={tratoId}`
- THEN the system MUST set `Trato.estado = GANADO`
- AND this outcome transition MUST NOT depend on Kanban column movement

#### Scenario: Losing a trato

- GIVEN a `Trato` with `estado = ABIERTO`
- WHEN the user sends `PUT /api/tratos/perder?id={tratoId}` with a valid loss reason
- THEN the system MUST set `Trato.estado = PERDIDO`
- AND this outcome transition MUST NOT depend on Kanban column movement
