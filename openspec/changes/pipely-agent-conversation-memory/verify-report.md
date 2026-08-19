# Verification Report — Gated Tablero/Columna/Ficha Tool Corrective Pass

**Change:** `pipely-agent-conversation-memory`
**Date:** 2026-08-19
**Branch:** `feat/agent-tablero-columna-ficha-tools`

## Current verdict

**Narrow corrective scope passes; broad baseline remains non-green and was not rerun.** The implementation defaults to the six-tool production catalog and exposes the 15 tablero/columna/ficha callbacks only when `crm2.agent.development-tools-enabled=true` and exactly one active profile is `noauth` or `test` (21 total). Production, profile-less, unknown, `noauth,test`, and every other mixed-profile true fails startup. No delete/remove callback is registered.

## Corrective evidence implemented

- Distinct optional trusted `superUsuarioId` propagation through command/service/adapter `ToolContext`; model schemas exclude it.
- Positive super-user and negative normal-user `create_columna` behavior.
- Deterministic caps/order, truthful metadata, typed ID extraction, and defensive list copies.
- Required full-replacement `edit_ficha.etiquetaIds` semantics.
- Stable Jackson-serialized Spring AI tool errors wired into the actual two-response ChatClient loop through one explicit manager/advisor owner.
- The exact-profile matrix distinguishes absent/default, explicit false, `noauth`-only and `test`-only acceptance, plus profile-less, unknown, production, `noauth,test`, and accepted-plus-production rejection. Successful actual ChatClient requests compare exact model-received callback names with exact rendered prompt catalog names.
- Sentinel-backed `find_contacts` reports `returned`/`truncated`; fully materialized development lists report exact `total`/`truncated`.

## Known production blockers

The 15 opt-in tools remain actor/tenant unscoped in their backing Application contracts. `edit_trato` is also actor-free downstream. The existing durable `AgentToolAction` ledger cannot atomically span the CRM mutation and ledger completion; a crash between them can duplicate a retry. No idempotency guarantee is claimed and no in-memory substitute was added.

## Verification evidence

| Command / suite | Result |
|---|---:|
| Application completion contract/service tests | **15/15 passed** |
| Infrastructure mapper/tool/adapter/identity tests plus `AgentConversationIT`; representative base/development callbacks serialize canonical record component keys through `ToolCallback.call(...)` | **89/89 passed** |
| Boot agent configuration/wiring tests | **42/42 passed** |
| Real two-response redaction and safe-validation loop tests | **2/2 passed** (within `AgentConfigTest`) |
| Exact-profile environment/configuration tests | **9/9 passed** (10 contexts within `AgentDevelopmentToolsConfigTest`) |
| Actual ChatClient callback/prompt synchronization | **4/4 passed contexts**: absent/default 6, explicit false 6, accepted `noauth` true 21, accepted `test` true 21 |
| Focused `AgentConversationIT` | **6/6 passed** |
| Affected Infrastructure/Boot reactor package (`-DskipTests`; focused tests run separately) | **PASS** |
| `git diff --check` after final correction/evidence refresh | **PASS** |

The broad verify was intentionally not rerun in this narrow serialization correction. The preserved prior baseline reached Infrastructure integration tests at **54/55 passed**; its only failure was the pre-existing `TableroControllerIT.create_shouldReturn201WithTableroJson` authorization mismatch (expected 201, received 403), after which Boot was skipped. The focused `AgentConversationIT` reactor run in this pass passes 6/6.

Callback catalogs are exact in successful modes: absent/default = **6**, explicit false under `prod` = **6**, accepted `noauth` true = **21**, and accepted `test` true = **21** (the six defaults plus 15 development callbacks). All successful modes explicitly assert absence of delete/remove names; profile-less, unknown, production, `noauth,test`, and accepted-plus-production true reject configuration creation.

Exact changed-line count including untracked files: **2,582** (**2,082 additions + 500 deletions**).

## Seven-slice Feature Branch Chain

1. Trusted identity transport.
2. Safe default-tool execution loop.
3. Bounded output contracts.
4. Read-only development callbacks (unregistered).
5. Tablero/columna mutations (unregistered).
6. Ficha mutations (unregistered).
7. Environment-safe registration and catalog integration.

Target at most 400 changed lines per child where technically feasible. Exact per-slice counts remain unknown until staging proves them. Shared files require hunk-level staging or reconstruction, every intermediate branch must compile and pass focused tests, and documentation/verification must travel with the behavior each slice introduces.
