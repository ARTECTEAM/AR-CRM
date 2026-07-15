# Proposal: Kanban Workflow State Source

## Intent

Problem: workflow state is ambiguous. TAREAS operational status must come from its Kanban column, TRATOS pipeline stage must come from its Kanban column, and `Trato.estado` must remain only the commercial outcome (`ABIERTO/GANADO/PERDIDO`).

## Scope

### In Scope
- Treat `Ficha.columnaId` + resolved column as source for TAREA status and TRATO pipeline stage.
- Validate ficha movement so TAREA fichas move only inside TAREAS columns and TRATO fichas only inside TRATOS columns.
- Keep `/tratos/ganar` and `/tratos/perder` as the only operations that change `Trato.estado`.
- Rename/seed TRATOS default columns as pipeline stages, not outcome labels.

### Out of Scope
- WhatsApp behavior.
- Adding `estado` to `Tarea` or pipeline-stage fields to `Trato`.
- Reintroducing `estado_tarea` / `estado_trato` database columns.
- UI implementation.

## Capabilities

### New Capabilities
- `ficha-workflow-movement`: validation and semantics for moving fichas between workflow columns.

### Modified Capabilities
- `tablero`: tablero/column data remains the workflow structure used to resolve status/stage.

## Approach

Use current Kanban model: `Ficha` points to the current column. Application movement service validates target column compatibility through existing/extended ports; infrastructure resolves column assignment/type. Domain preserves separation: `Tarea` has no estado; `Trato.estado` is commercial outcome only.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain` | Modified | `Tablero` defaults, `Ficha` movement invariants, `Trato.estado` outcome boundary. |
| `application` | Modified | `MoverColumnaFichaService` validates type/column assignment. |
| `infrastructure` | Modified | Repository adapters resolve target column/tablero metadata and seed names. |
| `boot` | Unchanged | Wiring only if a port implementation is added. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Existing TRATOS columns use outcome-like names | Med | Add safe/idempotent migration or seed adjustment. |
| Invalid existing fichas in wrong board type | Low | Detect before enforcing or fail with clear 422/409. |
| Confusing stage with outcome | Med | Specs must explicitly separate pipeline stage from `Trato.estado`. |

## Rollback Plan

Revert movement validation and seed/migration changes. Existing `Ficha.columnaId` and `Trato.estado` data remain intact because no new state columns are introduced.

## Dependencies

- Frontend change `kanban-workflow-state-source` consumes the clarified contract.

## Success Criteria

- [ ] Moving fichas across incompatible board types is rejected.
- [ ] TAREA status and TRATO pipeline stage are derived from columns.
- [ ] Moving TRATO columns never changes `Trato.estado`.
