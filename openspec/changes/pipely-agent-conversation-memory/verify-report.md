# Verification Report — Corrective Child C1 (Second Fresh-Context Verification)

**Change**: `pipely-agent-conversation-memory`

**Scope**: Corrected C1 tasks 6.1–6.5 only

**Mode**: Strict TDD

**Artifact store**: OpenSpec

**Date**: 2026-08-04

**Branch / HEAD / base**: `feat/pipely-agent-conversation-memory-pr9c4-c1-authorization` / `5d3e92523a4228313a9d67894cb3dde2244d41cb` / `5d3e925`

**C2**: Tasks 7.1–7.6 intentionally excluded

## Executive Result

**Verdict: FAIL.** Corrected C1 itself is behaviorally compliant, complete, within budget, and supported by adequate Strict-TDD evidence. Fresh focused execution proves trusted `(agentOwnerId, actorUsuarioId, turnId)` propagation through non-model-visible Spring AI `ToolContext`, Application-layer `AgentCrmWriteUseCase` delegation, strict `responsableId == actorUsuarioId` authorization before the existing mutation path, and fail-closed malformed owner/turn handling with zero Application calls. Independent Git measurement is **775 A+D**, satisfying the hard 800 limit.

The final gate remains blocked because the configured broad `mvnw.cmd verify` command exits 1 with 31 independently reproduced, pre-existing test-context errors: 21 in `SecurityConfigTest` from a missing `WaApiKeyFilter` bean and 10 in `UsuarioControllerMvcTest` from a missing `FindBotByTokenUseCase` bean. Neither failing test class nor missing collaborator is in the C1 diff, but Strict-TDD verification classifies a non-zero configured broad gate as CRITICAL.

No C2 write-idempotency scenario and no v2 ambiguous durable-memory update/supersession behavior is counted as a C1 failure.

## Completeness

| Metric | Result |
|---|---:|
| Scoped tasks 6.1–6.5 | 5 |
| Checked / verification-satisfied | 5 / 5 |
| Applicable C1 spec scenarios | 13 / 13 compliant |
| Excluded C2 tasks 7.1–7.6 | 6 / 6 correctly unchecked |
| Hard review budget | 775 / 800 A+D — PASS |

## Build, Test, and Coverage Evidence

| Command | Exit | Fresh result |
|---|---:|---|
| `.\mvnw.cmd -pl application -am test "-Dtest=AgentCrmWriteServiceTest,CompleteUserTurnServiceTest,DurableMemoryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` | 0 | **18/18 passed**: authorization 4 methods, turn completion 8, durable memory 6. The authorization loop executes both unauthorized statuses. |
| `.\mvnw.cmd -pl infrastructure test "-Dtest=SpringAiCrmToolsTest,SpringAiChatCompletionAdapterTest,CrmToolMapperTest,AgentControllerMvcTest,AgentConversationIT"` | 0 | **66/66 passed**: tools 21, completion adapter 14, mapper 14, MVC 11, H2/security conversation 6. |
| `.\mvnw.cmd -pl infrastructure test "-Dtest=SpringAiCrmToolsTest#malformedTrustedWriteContextFailsClosedBeforeApplicationUseCaseInvocation"` | 0 | **1/1 passed** at the real Spring AI callback boundary; five fixed invalid contexts each throw and the write use case is never called. This is a duplicate focused characterization rerun, not an additional unique test. |
| `.\mvnw.cmd -pl infrastructure test "-Dtest=AgentTurnCompletionPersistenceAdapterTest,DurableMemoryPersistenceAdapterTest,AgentToolActionPersistenceAdapterTest"` | 0 | **21/21 passed**: companion H2 turn 6, durable memory 6, pre-C2 ledger 9. |
| `.\mvnw.cmd -pl boot -am test "-Dtest=AgentConversationWiringTest,AgentConfigTest,AgentConfigOpenAiWiringTest" "-Dsurefire.failIfNoSpecifiedTests=false"` | 0 | **26/26 passed**: composition 10, config 11, no-network OpenAI wiring 5. |
| `.\mvnw.cmd -pl infrastructure verify "-Dtest=SpringAiCrmToolsTest,SpringAiChatCompletionAdapterTest,CrmToolMapperTest,AgentControllerMvcTest,AgentConversationIT" "-Dit.test=AgentConversationIT"` | 0 | **66 Surefire + 6 Failsafe executions passed**; JaCoCo report generated. |
| `.\mvnw.cmd -DskipTests package` | 0 | Six-module Java 21 compile/package passed. |
| `.\mvnw.cmd verify` | **1** | Domain **225/225** and Application **150/150** passed. Infrastructure ran 385 tests with 0 failures and **31 errors**; Boot was skipped. |
| `.\mvnw.cmd -pl infrastructure test "-Dtest=SecurityConfigTest"` | 1 | Reproduced **21/21 errors**: `SecurityConfig.apiSecurityFilterChain` cannot resolve `WaApiKeyFilter`. |
| `.\mvnw.cmd -pl infrastructure test "-Dtest=UsuarioControllerMvcTest"` | 1 | Reproduced **10/10 errors**: component-scanned `BotApiTokenFilter` cannot resolve `FindBotByTokenUseCase`. |

### Broad Failure Causality

The broad failures reduce exactly to the two focused reproductions above. C1 changes neither failing test class, `SecurityConfig`, `WaApiKeyFilter`, `BotApiTokenFilter`, nor `FindBotByTokenUseCase`. The focused C1 Application, tool/adapter/MVC/H2, companion persistence, Boot, coverage, and package gates all pass. The 31 errors are therefore baseline test-harness defects rather than C1 regressions. They remain a release-gate blocker under the verification policy.

## Behavioral Compliance Matrix

| Capability | Applicable amended scenario | Fresh passing runtime evidence | Status |
|---|---|---|---|
| security | Missing credentials are rejected before protected work | `AgentConversationIT#anonymous_isRejectedByRealSecurityChain_beforeAnyUseCaseRuns` in the 66/66 Infrastructure run | ✅ COMPLIANT |
| security | Owner override cannot cross isolation boundaries | `AgentConversationIT` different-owner isolation plus MVC request mapping and model-visible no-leak tests | ✅ COMPLIANT |
| security | CRM authorization remains enforced for every applicable C1 tool path | `AgentCrmWriteServiceTest` unauthorized GANADO/PERDIDO; zero mutator calls | ✅ COMPLIANT |
| agent-crm-tools | Unknown/aliased tools are rejected | Spring AI discovery exposes exactly `find_contacts`, `create_contact`, and `update_deal_stage` | ✅ COMPLIANT |
| agent-crm-tools | Contact search respects current visibility | `SpringAiCrmToolsTest` trusted actor forwarding and bounded search-output cases | ✅ COMPLIANT |
| agent-crm-tools | Contact creation uses trusted identity | Tool callback tests prove actor comes from `ToolContext`, not schema/model arguments; C2 retry clause excluded | ✅ COMPLIANT for C1 |
| agent-crm-tools | Deal-stage authorization and validation are enforced | Authorized GANADO/PERDIDO, unsupported status, missing deal, and unauthorized actor cases pass | ✅ COMPLIANT |
| agent-conversation | Valid request returns final content after owner-scoped preparation | `AgentControllerMvcTest` and `AgentConversationIT` | ✅ COMPLIANT |
| agent-conversation | Same normalized request retry does not duplicate visible state | Three-request H2 convergence and completion-call-count cases in `AgentConversationIT` | ✅ COMPLIANT |
| agent-conversation | Completion, regeneration, and tool execution have no public operation | MVC 404/405 cases and sole-controller inspection | ✅ COMPLIANT |
| agent-durable-memory | Eligible memory is recalled separately from visible history | `DurableMemoryServiceTest` and `DurableMemoryPersistenceAdapterTest` | ✅ COMPLIANT |
| agent-durable-memory | Sensitive content without eligibility is not persisted | `DurableMemoryServiceTest` eligibility/rejection cases | ✅ COMPLIANT |
| agent-durable-memory | Repeated reconstruction is deterministic and duplicate-free | Application ordering cases and H2 owner-scoped ordering cases | ✅ COMPLIANT |

**Applicable C1 compliance: 13/13.** The CRM write-retry scenario belongs to C2 tasks 7.1–7.6. Ambiguous durable-memory update/supersession is explicitly v2.

## Correctness

| Requirement | Result | Evidence |
|---|---|---|
| Trusted owner/actor/turn tuple | ✅ | `CompleteUserTurnService` supplies server-derived values; `SpringAiChatCompletionAdapter` places them in `.toolContext(Map.of(...))`. |
| No model/request identity override | ✅ | Tool schemas and captured model instructions exclude identity values and key names; REST request DTO has no owner/actor/turn identity. |
| `update_deal_stage` uses Application boundary | ✅ | `SpringAiCrmTools` depends on `AgentCrmWriteUseCase` and forwards `UpdateDealStage`; no direct actor-free deal mutator dependency remains in the tool. |
| Strict deal ownership before effects | ✅ | `AgentCrmWriteService` loads the deal, checks `responsableId == actorUsuarioId`, then and only then delegates to the existing mutator. Unauthorized/missing cases make zero mutator calls. |
| Malformed owner/turn fails closed | ✅ | Real `ToolCallback.call` characterization covers missing owner, blank owner, wrong owner type, missing turn, and wrong turn type; all throw `ToolExecutionException`, with `AgentCrmWriteUseCase.execute` called zero times. |
| Exactly three DefaultTools | ✅ | Runtime callback discovery finds exactly three names; `AgentConfig` registers the shared bean once with `defaultTools(tools)` and no request `.tools(...)` call exists. |
| One endpoint | ✅ | Only `POST /api/agent/messages` exists for the agent; completion/regeneration/tool routes remain internal. |
| No network | ✅ | Deterministic ChatModel, MVC/H2, and Boot suites pass without provider credentials or network access. |

## Design Coherence

| Design decision | Result | Notes |
|---|---|---|
| `boot → infrastructure → application → domain` | ✅ | Authorization is in Application; Spring AI adaptation is in Infrastructure; Boot only composes. |
| Trusted identity stays outside schemas | ✅ | Framework `ToolContext` carries the tuple. |
| Strict ownership precedes actor-free mutator | ✅ | Application service gates before the existing save/event/note path. |
| No invented role override | ✅ | Equality with `responsableId` is the sole C1 authorization rule. |
| Shared three-tool defaults and one ingress | ✅ | Runtime discovery, Boot wiring, and MVC evidence pass. |
| C2 atomic ledger integration | ➖ EXCLUDED | Correctly absent from C1 and unchecked in tasks. |

## TDD Compliance

The cumulative Engram topic `sdd/pipely-agent-conversation-memory/apply-progress` contains a formal task table with explicit **SAFETY NET, RED, GREEN, TRIANGULATE, and REFACTOR** columns and reproducible commands/counts. Its pre-correction safety-net counts match the previous report, and its corrected GREEN counts exactly match this fresh execution: 18 Application, 66 primary Infrastructure, 21 companion H2, 26 Boot, 66 Surefire + 6 Failsafe coverage executions, and successful package/type check.

Historical evidence is classified without rewriting history: the new Application contracts naturally produced compile-time RED because their production types did not yet exist; owner/turn propagation had a separate runtime behavioral RED. A compile-time contract RED is valid tests-first structural evidence, but it is not represented as runtime behavioral proof. The corrective malformed-context work is honestly recorded as characterization/triangulation because production already failed closed; no defect or fabricated RED was manufactured. Current runtime scenario evidence supplies the required behavioral proof.

| Check | Result | Details |
|---|---|---|
| Formal TDD evidence reported | ✅ | 5/5 C1 rows; all required columns present. |
| Test files exist | ✅ | All named Application, Infrastructure, and Boot files exist. |
| RED classified honestly | ✅ | Natural compile-time RED for absent new contracts; separate runtime RED for owner/turn propagation; characterization is not mislabeled RED. |
| GREEN confirmed now | ✅ | 5/5 tasks' focused files pass in fresh execution. |
| Triangulation adequate | ✅ | Authorized/denied status variants, missing deal, trusted propagation, schema/no-leak, five malformed contexts, MVC/H2, and wiring cases. |
| Safety net auditable | ✅ | Exact pre-correction commands/counts are present and consistent with prior verification evidence. |
| Refactor evidence | ✅ | Git measurement confirms the reduced 775 A+D slice; retained scenarios still pass. |
| Broad safety gate | ❌ | Configured `mvnw.cmd verify` exits 1 with 31 causally isolated baseline context errors. |

**Strict-TDD judgment: implementation-cycle evidence PASS; configured broad gate FAIL.** No policy noncompliance is assigned merely because a genuinely new Java contract first failed compilation, and no runtime RED is invented for already-correct fail-closed behavior.

## Test Layer Distribution

| Layer | Fresh selected tests | Files / tools |
|---|---:|---|
| Application unit | 18 | JUnit/Mockito; includes 4 authorization methods and both denied statuses |
| Infrastructure unit/component/MVC/H2 | 66 | Spring AI callbacks, deterministic ChatModel, MockMvc, Spring Security, H2 |
| Companion H2 persistence | 21 | Turn, durable-memory, and pre-C2 ledger adapters |
| Boot composition/config | 26 | Spring context and deterministic/mock external seams |
| External-provider E2E | 0 | Not configured; no network required |
| **Primary unique selected executions** | **131** | Excludes duplicate one-method rerun and Failsafe repeat |

## Changed-File Coverage

Fresh JaCoCo data is available for Infrastructure and Boot. Application does not configure a module JaCoCo report, so its new files have passing runtime tests but no numeric coverage.

| Changed production file | Line | Branch | Uncovered executable lines | Rating |
|---|---:|---:|---|---|
| `infrastructure/.../SpringAiChatCompletionAdapter.java` | 100% (24/24) | 100% (4/4) | — | ✅ Excellent |
| `infrastructure/.../tool/SpringAiCrmTools.java` | 95.5% (42/44) | 85.0% (17/20) | 120, 136 | ✅ Excellent lines |
| `boot/.../WiringConfig.java` | 98.7% (153/155) | N/A | 2 whole-file lines; C1 wiring is exercised | ✅ Excellent |
| New Application command/port/service/exception | N/A | N/A | No JaCoCo report in `application/pom.xml` | ➖ Not available |

Configured threshold is 0. Coverage is informational and does not replace the passing scenario evidence.

## Assertion Quality

The changed C1 test paths contain no tautology, assertion without production execution, possibly-empty ghost loop, smoke-only proof, or mock-heavy false positive. The malformed-context loop iterates a fixed five-element list and calls the real callback on every iteration before asserting the wrapper exception; the final zero-call verification covers the complete loop.

Two pre-existing type-only patterns remain in files touched for C1 composition changes:

| File | Pattern | Severity |
|---|---|---|
| `AgentConfigTest.java` | Standalone configured-client `isNotNull()`; companion behavioral/default-tool tests mitigate it | WARNING |
| `AgentConversationWiringTest.java` | Five durable-memory bean `isNotNull()` assertions; unrelated to the new C1 concrete-type/singleton assertion | WARNING |

**Assertion quality: 0 CRITICAL, 2 WARNING.**

## Review Workload Audit

Git numstat semantics against `5d3e925`, including tracked changes, every untracked C1 file, and `tasks.md`, excluding only generated `verify-report.md`:

| Boundary | Additions | Deletions | A+D |
|---|---:|---:|---:|
| Tracked C1 paths and `tasks.md` | 360 | 202 | 562 |
| Untracked Application files | 213 | 0 | 213 |
| **Total C1** | **573** | **202** | **775** |

**Budget result: PASS — 775 ≤ 800, with 25 lines of headroom.** `git diff --check 5d3e925` excluding the generated report is clean; Git emitted only line-ending conversion warnings, not whitespace errors.

## Quality Metrics

**Linter**: ➖ Not available.

**Type checker/build**: ✅ Six-module Java 21 package passed.

**Coverage threshold**: ✅ Configured threshold 0.

**C2/v2 exclusions**: ✅ Not misclassified as C1 failures.

## Issues Found

### CRITICAL

1. **Configured broad gate is non-zero.** `mvnw.cmd verify` exits 1 with 31 reproduced baseline test-context errors. Causality is outside C1, but the mandatory verification command remains blocking.

### WARNING

1. Two pre-existing type-only assertion patterns remain in changed Boot test files; companion behavioral assertions prevent them from invalidating C1 evidence.

### SUGGESTION

None for C1. Repair the two baseline test fixtures in their own authorized scope, then rerun the broad gate without altering this verified C1 behavior.

## Final Verdict

**FAIL**

C1 behavior, task completion, Strict-TDD evidence, scenario coverage, architecture, and the 775 A+D budget all pass. Delivery remains blocked solely by the mandatory broad `mvnw.cmd verify` failure caused by 31 causally isolated baseline test-context errors outside C1.
