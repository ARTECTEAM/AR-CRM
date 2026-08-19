# Proposal: Pipely Memory-Capable CRM Action Agent

> **Layers:** application | infrastructure | boot

## Intent

Provide one authenticated CRM conversation with durable memory and a deliberately bounded tool surface. The production-safe default remains small; unfinished board, column, and card capabilities are available only through an explicit development opt-in.

## Scope

### Production default

- `POST /api/agent/messages` derives immutable identity from JWT `ActorContext`.
- Exactly six registered and advertised tools: `find_contacts`, `create_contact`, `edit_contact`, `create_company`, `edit_company`, and `edit_trato`.
- Visible history and durable memory remain owner scoped and bounded.

### Opt-in development catalog

Setting `crm2.agent.development-tools-enabled=true` adds exactly 15 non-delete tools only when exactly one Spring profile is active and it is explicitly recognized as non-production (`noauth` or `test`): `list_tableros`, `get_tablero`, `create_tablero`, `edit_tablero`, `assign_columna_to_tablero`, `reorder_tablero_columns`, `list_columnas`, `get_columna`, `create_columna`, `edit_columna`, `list_fichas`, `get_ficha`, `create_ficha`, `edit_ficha`, and `move_ficha_to_columna`. The default is `false`; these callbacks are neither registered nor advertised otherwise. Flag `true` with no active profile, `noauth,test`, `prod`, `production`, or any other unrecognized/mixed profile fails startup with a stable configuration error.

The additional tools are development-only because current tablero/columna/ficha Application contracts do not enforce actor/tenant ownership. Their mutating paths also lack an atomic durable action-to-business-write convergence transaction. Existing `AgentToolAction` persistence can durably claim and complete an action, but cannot atomically include these CRM writes; a crash after mutation and before ledger completion can still duplicate a retry. No process-local substitute is introduced.

## Security and identity

- `usuarioId` and optional `superUsuarioId` are separate trusted claims and remain outside generated schemas.
- `create_columna` passes only the actual trusted optional `superUsuarioId`. A normal user is never converted into `SuperUsuarioId`; `PREDETERMINADA` creation fails without the trusted claim, while `PERSONALIZADA` creation remains available to a normal authenticated actor.
- `edit_trato` and all 15 development tools still have documented downstream actor/tenant authorization gaps. Trusted-context presence is not represented as target authorization.
- Tool failures are processed inside the real ChatClient tool loop into stable redacted model-visible results. Explicitly marked validation failures receive a fixed safe code/message; arbitrary exception text is never copied.

## Out of Scope / Production blockers

- Actor/tenant persistence and authorization redesign for tablero, columna, ficha, and `edit_trato`.
- An atomic durable convergence boundary spanning the action ledger and each CRM mutation.
- Delete/remove callbacks, arbitrary discovery, RAG, MCP, streaming, or public completion/regeneration routes.

## Success criteria

- Absent/default or explicit-false configuration registers and advertises exactly six callbacks in every environment; explicit development opt-in under `noauth` or `test` registers and advertises exactly 21; production/unrecognized true fails startup.
- No delete/remove callback exists in either mode.
- Trusted identity cannot be supplied by model arguments, and arbitrary runtime exception text is not model visible.
- Read outputs are deterministically ordered, hard-capped, and immutable. Sentinel-backed `find_contacts` reports `returned`/`truncated` without claiming an exact total; fully materialized development lists report exact `total`/`truncated`.
- Production exposure of the 15 development tools remains blocked until both authorization and durable mutation convergence are implemented and re-reviewed.

## Feature Branch Chain

Deliver in seven dependency-ordered slices: (1) trusted identity transport; (2) safe default-tool execution loop; (3) bounded output contracts; (4) read-only development callbacks, unregistered; (5) tablero/columna mutations, unregistered; (6) ficha mutations, unregistered; and (7) environment-safe registration and catalog integration. Target at most 400 changed lines per child where technically feasible, but do not claim exact slice counts before staging. Shared files require hunk-level staging or reconstruction; every intermediate branch must compile and pass focused tests. Tests, documentation, and verification travel with the behavior introduced by each slice.
