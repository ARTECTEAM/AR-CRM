# Tasks: Pipely Agent Conversation Memory — Corrective Tool Pass

## Current branch and delivery

- Branch: `feat/agent-tablero-columna-ficha-tools`.
- No commit, push, rebase, merge, reset, stash, or branch switch is part of this pass.
- Review workload is above 400 changed lines. Use the dependency-ordered seven-slice Feature Branch Chain documented below. Target at most 400 changed lines per child where technically feasible; exact per-slice counts remain unproven until staging.
- Shared files require hunk-level staging or reconstruction. Every intermediate branch must compile and pass focused tests, and each behavior carries its tests, documentation, and verification evidence.

## Completed corrective work

- [x] Preserve the six-tool default catalog.
- [x] Separate the 15 tablero/columna/ficha callbacks behind `crm2.agent.development-tools-enabled=false`.
- [x] Require both the explicit true flag and exactly one recognized non-production profile (`noauth` or `test`); reject production, profile-less, unknown, `noauth,test`, or any other mixed-profile true at startup with a stable error.
- [x] Propagate the distinct optional trusted `superUsuarioId` claim outside model schemas.
- [x] Reject `PREDETERMINADA` column creation without that claim; preserve normal `PERSONALIZADA` creation.
- [x] Require `edit_ficha.etiquetaIds` and define full replacement (`[]` clears all).
- [x] Add deterministic caps, stable typed-ID ordering, truthful metadata, and defensive list copies.
- [x] Add a redacting Spring AI `ToolExecutionExceptionProcessor`.
- [x] Wire that processor into the real ChatClient tool loop through one explicit `ToolCallingManager` and one auto-registered `ToolCallingAdvisor`.
- [x] Prove redaction and explicit safe validation with two-response capturing-model loop tests.
- [x] Prove absent/default, explicit false, `noauth`-only and `test`-only acceptance, plus profile-less, unknown, production, `noauth,test`, and accepted-plus-production rejection; successful actual ChatClient requests synchronize callback/prompt catalogs at 6/21 without direct callback introspection from tool objects.
- [x] Keep all delete/remove callbacks absent.
- [x] Inspect the durable `AgentToolAction` ledger and document why it cannot atomically converge the current CRM writes.

## Verification checklist

- [x] Focused Application tests: 15 passed.
- [x] Focused Infrastructure mapper/tool/adapter/identity tests plus `AgentConversationIT`: 89 passed, including representative base/development `ToolCallback.call(...)` JSON serialization contracts.
- [x] Focused Boot agent configuration/wiring tests: 42 passed; real-loop redaction, the exact-profile environment matrix, and exact successful 6/21 callback/prompt catalogs asserted.
- [x] Affected Infrastructure/Boot reactor package with tests skipped (focused tests were run separately).
- [x] Known broad baseline preserved, not rerun in this narrow pass: the prior safe relevant suite had one independently reproduced pre-existing `TableroControllerIT` 403 failure and no corrective-scope regression.
- [x] Focused `AgentConversationIT`: 6 passed.
- [x] `git diff --check` after the final serialization correction and evidence refresh.
- [x] Actual changed-line count including untracked files: **2,582** (**2,082 additions + 500 deletions**).

## Dependency-ordered review chain

1. Trusted identity transport, with command/service/adapter tests and identity documentation.
2. Safe default-tool execution loop, with the configured manager/processor, two-response redaction tests, and error-policy documentation.
3. Bounded output contracts, with DTO/mapper tests and output-contract documentation.
4. Read-only development callbacks, with callback tests and documentation, still unregistered.
5. Tablero/columna mutations, with tests and documentation, still unregistered.
6. Ficha mutations, including full-replacement `etiquetaIds`, with tests and documentation, still unregistered.
7. Environment-safe registration and catalog integration, with the exact-profile matrix, actual request/prompt synchronization tests, and deployment/catalog documentation.

## Remaining production work

- [ ] Add actor/tenant ownership persistence and authorization to tablero/columna/ficha and actor-aware authorization to `edit_trato`.
- [ ] Design an atomic durable convergence boundary spanning action claim, CRM mutation, and canonical result; add repeat-invocation effect tests only after that boundary exists.
