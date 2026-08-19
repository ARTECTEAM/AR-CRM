# Verification Report — Author-Removal Final Corrective Pass

**Change**: `pipely-agent-conversation-memory`

**Scope**: Current six-tool agent/removal tree plus legacy `Trato` state migration.

**Date**: 2026-08-18

**Branch**: `feature/remove-julio-antonio-contributions`

## Executive Result

**Verdict: FAIL due to one unrelated baseline test failure.** The current six-tool tree, approved author removals, `GANADO`/`PERDIDO` data normalization, focused persistence coverage, package build, Spring AI suites, domain tests, and Boot wiring tests pass. The broad gate remains non-zero only because `TableroControllerIT.create_shouldReturn201WithTableroJson` expected 201 and received 403.

## Current Removal Surface

- Spring AI exposes exactly six CRM tools: `find_contacts`, `create_contact`, `edit_contact`, `create_company`, `edit_company`, and `edit_trato`.
- Removed WhatsApp, bot, notes, `find_companies`, filter-criteria, and deal win/loss application/domain/persistence surfaces remain removed.
- No removed enum values were reintroduced; `EstadoTrato` contains only `ABIERTO` and `CERRADO`.
- The shared `boot/src/main/resources/schema.sql` retains existing non-WA statements and adds an idempotent `tratos.estado` cleanup after Hibernate initialization.

## Migration and Persistence Evidence

| Test | Result | Evidence |
|---|---:|---|
| `TratoRepositoryIT` | **3/3 passed** | Both legacy values are inserted through JDBC, normalized twice to `CERRADO`, and hydrated through `TratoRepository`; `ABIERTO` remains unchanged. |
| `TratoTest` | **6/6 passed** | Current domain creation and reconstitution behavior. |

The H2 test changes the generated enum column to `VARCHAR(20)` before inserting legacy rows. This is necessary because H2 rejects removed enum values before the migration can run; it models PostgreSQL `EnumType.STRING` storage and proves the real repository/entity hydration path. PostgreSQL parser execution of the full shared script is not available in the local test environment.

## Focused Verification

| Command / suite | Result |
|---|---:|
| `mvnw.cmd -pl infrastructure test -Dtest=TratoRepositoryIT` | **3/3 passed** |
| `mvnw.cmd -pl domain test -Dtest=TratoTest` | **6/6 passed** |
| `mvnw.cmd -pl infrastructure test -Dtest=CrmToolMapperTest,SpringAiCrmToolsTest,SpringAiChatCompletionAdapterTest` | **69/69 passed** |
| `mvnw.cmd -pl boot -am test -Dtest=AgentConversationWiringTest,AgentConfigTest,AgentConfigOpenAiWiringTest` | **30/30 passed** |
| `mvnw.cmd -DskipTests package` | **PASS** |
| `mvnw.cmd verify` | **FAIL: 1 known baseline failure** |

The broad run reached Infrastructure and executed 55 tests with 1 failure and 0 errors; Boot was skipped after the Infrastructure failure. No new migration, enum-hydration, Spring AI, domain, or packaging failure was observed.

## Documentation Alignment

- `APPLICATION_RULES.md`: removed the obsolete `GANADO` setter and ganar transition example.
- `DOMAIN_RULES.md`: removed obsolete `marcarComoGanado`/`marcarComoPerdido` behavior claims.
- `openspec/changes/pipely-agent-conversation-memory/specs/agent-crm-tools/spec.md`: describes preserved deal state without a loss-reason contract.
- This report now describes the current six-tool/removal tree, migration coverage, package result, and the single known 403 baseline failure.

## Search Results

Intentional current legacy literals are limited to the migration SQL and its focused regression test: `GANADO` and `PERDIDO` appear only as values being normalized. Historical OpenSpec archives may retain old terminology and are not part of this corrective pass. The executable/runtime WhatsApp and Evolution surfaces are removed; retained references are limited to removal context and protective ignore rules for legacy generated uploads and a credential-bearing local startup script. Those safety/context references do not advertise active functionality. No active source/test contract retains `motivoPerdida`, ganar/perder behavior, or `find_companies`.

## Final Judgment

**Final review blocker: resolved in implementation.** Existing legacy PostgreSQL string values are normalized idempotently before current enum hydration, and repository regression coverage passes.

**Delivery verification: not fully green** because the unrelated known `TableroControllerIT` 403 baseline remains.
