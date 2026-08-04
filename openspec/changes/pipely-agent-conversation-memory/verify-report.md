# Verification Report — Conversational Agent Tasks 4.1–5.2

**Change**: `pipely-agent-conversation-memory`
**Scope**: Tasks 4.1–5.2; current delta specs remain authoritative
**Mode**: Strict TDD
**Artifact store**: OpenSpec
**Date**: 2026-08-04
**Branch / HEAD**: `feat/pipely-agent-conversation-memory-pr9c2-final-conversation` / `0c93873808ebeda3bb732432f299fe73e3fe52e0`

## Executive Result

**Verdict: FAIL.** All four scoped tasks are present, the focused conversation, application, infrastructure-agent, and Boot composition suites pass, and the six-module Java 21 package/type gate succeeds. The final composition exposes the real turn and durable-memory adapters and the real Spring AI completion adapter, while `AgentConfig` registers the shared three-tool allowlist. Verification is nevertheless blocked by the mandatory `mvnw.cmd verify` exit 1 and by current-spec scenarios with no passing behavioral proof: denied CRM permission/ownership, idempotent CRM write replay, and ambiguous durable-memory update behavior. Source inspection also shows the tool-action ledger is wired but not connected to `SpringAiCrmTools`, and `update_deal_stage` validates but does not pass the trusted actor to the existing mutation use case.

## Completeness

| Metric | Value |
|---|---:|
| Scoped tasks | 4 |
| Complete | 4 |
| Incomplete | 0 |
| Explicit v2 deferrals excluded | Yes, except where a current delta spec expressly requires the behavior |

## Build and Test Execution

| Command | Exit | Fresh runtime evidence |
|---|---:|---|
| `.\mvnw.cmd -pl boot test "-Dtest=AgentConversationWiringTest" ...` | 0 | 9/9 passed. |
| `.\mvnw.cmd -pl boot test "-Dtest=AgentConfigTest,AgentConfigOpenAiWiringTest,AgentConversationWiringTest" ...` | 0 | 25/25 passed: 11 AgentConfig, 5 OpenAI wiring, 9 final composition. |
| `.\mvnw.cmd -pl infrastructure verify "-Dtest=AgentControllerMvcTest" "-Dit.test=AgentConversationIT" ...` | 0 | 17/17 passed: 11 MVC and 6 H2/security/callback integration cases; JaCoCo report generated. |
| `.\mvnw.cmd -pl application test "-Dtest=CompleteUserTurnServiceTest,DurableMemoryServiceTest" ...` | 0 | 14/14 passed. |
| `.\mvnw.cmd -pl infrastructure test "-Dtest=SpringAiCrmToolsTest,SpringAiChatCompletionAdapterTest,AgentTurnCompletionPersistenceAdapterTest,DurableMemoryPersistenceAdapterTest,AgentToolActionPersistenceAdapterTest" ...` | 0 | 52/52 passed. |
| `.\mvnw.cmd -DskipTests package` | 0 | All six reactor modules compiled and packaged with Java 21. |
| `.\mvnw.cmd verify` | 1 | Infrastructure: 381 tests, 0 failures, 31 errors; Boot skipped. |
| `.\mvnw.cmd -pl boot verify "-Dtest=AgentConfigTest,AgentConfigOpenAiWiringTest,AgentConversationWiringTest" ...` | 0 | 25/25 passed; Boot JaCoCo report generated. |

**Build/type check**: ✅ Passed.
**Focused scoped verification**: ✅ Passed.
**Configured broad verification**: ❌ Failed. The 31 errors are 21 `SecurityConfigTest` context errors caused by a missing `WaApiKeyFilter` test bean and 10 `UsuarioControllerMvcTest` context errors caused by a missing `FindBotByTokenUseCase` test bean. These failures are outside the agent implementation path and reproduce the known baseline class of failures, but the verify policy makes any non-zero required command CRITICAL. The broad reactor stopped in Infrastructure, so Boot and its unrelated `FichaWiringTest` baseline were not evaluated by this command.

## Production Composition Evidence

| Concern | Runtime/static proof | Result |
|---|---|---|
| Turn persistence | `AgentConversationWiringTest` creates the production `AgentTurnAdapter`, verifies all four outbound port types, and proves exactly one bean for each port. | ✅ |
| Durable memory | The same context creates the production `DurableMemoryPersistenceAdapter`, verifies both memory-port namespaces and five lifecycle ports, and reflectively proves `CompleteUserTurnService` received that exact adapter. H2 persistence tests pass 6/6. | ✅ |
| Completion | `WiringConfig.chatCompletionPort` returns `SpringAiChatCompletionAdapter`; the wiring test proves the configured `ChatCompletionPort` instance is injected into `CompleteUserTurnService`; adapter behavior passes 13/13. The harness mocks only `ChatClient`, not the adapter/service. | ✅ layered proof |
| OpenAI/Spring AI client | `AgentConfigOpenAiWiringTest` exposes exactly one qualified `ChatModel` and one `ChatClient`; 5/5 pass without network. | ✅ |
| CRM tools | `AgentConfigTest` and `SpringAiCrmToolsTest` prove one shared stateless tools object and exactly `find_contacts`, `create_contact`, and `update_deal_stage`; request identity travels through `ToolContext`. | ✅ layered proof |
| Full Boot path | The endpoint H2 test uses production turn persistence and real `find_contacts` callback discovery, but substitutes a constant memory port and a completion harness. The Boot wiring test uses real services/adapters with mocked repositories and `ChatClient`. No single test executes the entire final Boot context end-to-end. | ⚠️ Partial integration depth |

The 5.x production diff adds **11** beans: three persistence adapters, one UTC clock, two turn use cases, and five durable-memory use cases. Runtime uniqueness is asserted for 12 outbound port types (4 turn, 5 durable-memory, 3 tool-ledger). The five durable-memory input use cases are autowired by contract but only asserted non-null; exact singleton counts are not asserted for those input interfaces or for `ChatCompletionPort`.

## Spec Compliance Matrix

| Capability | Scenario | Passing runtime evidence | Result |
|---|---|---|---|
| security | Valid protected path authorizes; missing credentials return 401 before work | `AgentConversationIT#anonymous_isRejected...`; authenticated H2/MVC cases | ✅ COMPLIANT |
| security | Owner override cannot access another owner's conversation, memory, data, or tool results | Two-owner H2 isolation; JWT/body command capture; tool schemas exclude identity; per-call actor isolation | ✅ COMPLIANT (layered) |
| security | Actor lacking CRM permission/ownership cannot disclose or mutate target | No covering passing test. `update_deal_stage` discards the validated actor after `requireActor`; `CambiarEstadoTratoUseCase` accepts only deal id/status data. | ❌ UNTESTED |
| agent-durable-memory | Eligible memory is recalled independently of visible-history window | `CompleteUserTurnServiceTest`; `DurableMemoryPersistenceAdapterTest`; Boot injection identity test | ✅ COMPLIANT (layered) |
| agent-durable-memory | Sensitive content without eligible request is not persisted | `DurableMemoryServiceTest#rejectsUnsafeMemoryWithoutPersistingIt` | ✅ COMPLIANT |
| agent-durable-memory | Ambiguous update supersedes nothing; repeated reconstruction preserves order | Stable owner-scoped order passes in H2, but no test exercises an ambiguous update or proves it supersedes nothing. | ❌ UNTESTED |
| agent-crm-tools | Unallowlisted or aliased tool is not executed | Runtime Spring AI discovery exposes exactly three names | ✅ COMPLIANT |
| agent-crm-tools | `find_contacts` returns matching contacts visible to actor | Tool command captures trusted actor/cap; contact application/repository tests passed during broad execution | ✅ COMPLIANT (layered) |
| agent-crm-tools | `create_contact` returns canonical contact for required data and trusted actor | `SpringAiCrmToolsTest#createContactDelegates...` | ✅ COMPLIANT |
| agent-crm-tools | Visible deal becomes `GANADO`; unsupported status mutates nothing | Routing and unsupported-status no-mutation tests pass, but visibility/permission is not exercised. | ⚠️ PARTIAL |
| agent-crm-tools | Retried write identity converges without another effect | Ledger persistence replay tests pass in isolation, but no production tool consumes the ledger/action identity. `CreateContactoService` always creates/saves a new UUID; `CambiarEstadoTratoService` writes another event note on replay. | ❌ UNTESTED |
| agent-conversation | Valid request returns final content after owner-scoped context preparation | MVC happy path; H2 final persistence/callback; application orchestration; Boot composition | ✅ COMPLIANT (layered) |
| agent-conversation | Same normalized retry creates no duplicate visible messages/response | Three-request H2 convergence and completion-call-count proof | ✅ COMPLIANT |
| agent-conversation | Completion, regeneration, and tools have no public operation | MVC 404/405 cases and controller inspection | ✅ COMPLIANT |

**Compliance summary**: 10 compliant, 1 partial, 3 untested. Under the verification policy, every untested current-spec scenario is CRITICAL.

## Correctness

| Requirement | Status | Notes |
|---|---|---|
| One authenticated ingress | ✅ Implemented | Only `POST /api/agent/messages`; request contains message and idempotency key only. |
| Trusted identity | ✅ Implemented | JWT-derived `ActorContext`; no development fallback in the modified filter; actor UUID travels through completion to `ToolContext`. |
| Visible-history convergence | ✅ Implemented | Real H2 tests prove one USER and one ASSISTANT message and canonical replay. |
| Durable-memory read path | ✅ Implemented | Production adapter is wired into completion and passes layered H2/application tests. |
| Shared default tools | ✅ Implemented | Three discovered callbacks; request code does not replace defaults with `.tools(...)`. |
| Tool permission propagation | ❌ Incomplete | `find_contacts` and `create_contact` carry actor data, but `update_deal_stage` cannot pass actor to the mutation use case. |
| CRM write idempotency | ❌ Incomplete | The action ledger is a disconnected bean; tool methods neither claim nor complete an action. |
| Response minimization | ✅ Implemented | Public response exposes only `content`. |

## Design Coherence

| Decision | Followed? | Notes |
|---|---|---|
| `boot → infrastructure → application → domain` | ✅ Yes | Boot composes concrete adapters and Application contracts; no inner-layer framework dependency was introduced. |
| One authenticated REST ingress | ✅ Yes | Exact path and absent internal routes pass at runtime. |
| Shared stateless `SpringAiCrmTools` registered as defaults | ✅ Yes | Boot/AgentConfig tests and callback discovery pass. |
| Per-request trusted tool context | ✅ Yes | Adapter uses `.toolContext(...)` without request `.tools(...)`. |
| Reuse real turn and durable-memory services/adapters | ✅ Yes | Exact adapter identity is proved in the Boot context. |
| Final H2 test proves history, durable memory, three tools, and final persistence | ⚠️ Deviates | It proves history/final persistence, uses a constant memory port, and executes only `find_contacts`; companion tests cover the real memory adapter and all three tool definitions separately. |
| Deferred deeper deal authorization/write convergence | ❌ Conflicts with specs | Design marks these deferred, but current `security` and `agent-crm-tools` delta specs require permission/ownership and idempotent write replay. Specs take precedence. |

## TDD Compliance

Engram `sdd/pipely-agent-conversation-memory/apply-progress` was retrieved in full. It records cumulative 4.x evidence and a formal 5.x TDD table. Task 5.1 RED was an ApplicationContext failure caused by the genuinely missing `AgentTurnAdapter` bean; the base-to-working-tree diff corroborates that the bean did not exist before 5.2. Current GREEN is independently reproduced at 9/9. Task 5.2 safety suites are independently reproduced. Current 4.x test files exist and pass 17/17; prior corrective RED/GREEN history is documented in the cumulative apply artifact and the pre-5.x report.

| Check | Result | Details |
|---|---|---|
| TDD evidence reported | ✅ | Engram apply-progress available; 5.x formal table plus cumulative 4.x narrative/history. |
| All scoped tasks have tests/evidence | ✅ | 4/4 tasks; RED tasks 4.1 and 5.1 have test files and recorded failures. |
| 5.1 RED behavioral | ✅ | Recorded exit 1 due missing `AgentTurnAdapter`, not compilation failure; baseline diff corroborates missing bean. |
| GREEN current | ✅ | 9/9 wiring, 25/25 Boot, 17/17 MVC/H2, 14/14 application, 52/52 infrastructure agent. |
| Triangulation | ✅ | 9 wiring cases plus companion MVC, H2, application, adapter, and tool tests. |
| Safety net | ✅ scoped / ❌ broad | All focused safety suites pass; configured broad gate exits 1 on unrelated test-context omissions. |
| Refactor | ✅ | Final 5.x production change remains composition-only; no unrelated source repair was introduced. |

**TDD compliance**: 6 scoped checks satisfied; broad safety gate remains blocking.

## Test Layer Distribution

| Layer | Tests | Files | Tools |
|---|---:|---:|---|
| MVC/component (tasks 4.x) | 11 | 1 | `@WebMvcTest`, MockMvc |
| Integration (tasks 4.x) | 6 | 1 | `@SpringBootTest`, real security chain, H2, MockMvc |
| Boot composition (task 5.1) | 9 | 1 | `@SpringJUnitConfig`, real configuration/services/adapters, mocked external seams |
| Companion unit/integration safety suites | 91 | 10 | JUnit, Mockito, Spring AI deterministic model, H2 |
| External provider E2E | 0 | 0 | Not configured; no network required |

## Changed-File Coverage

Fresh focused JaCoCo reports:

| File | Line coverage | Branch coverage | Uncovered lines | Rating |
|---|---:|---:|---|---|
| `boot/.../WiringConfig.java` | 98.7% (152/154) | N/A | L305 (pre-existing), L1470 (`agentToolActionClock`) | ✅ Excellent |
| `infrastructure/.../AgentController.java` | 100% (9/9) | N/A | — | ✅ Excellent |
| `infrastructure/.../AgentMessageRequest.java` | 100% (1/1) | N/A | — | ✅ Excellent |
| `infrastructure/.../AgentMessageResponse.java` | 100% (2/2) | N/A | — | ✅ Excellent |
| `infrastructure/.../AgentRestMapper.java` | 100% (10/10) | 75.0% (3/4) | One branch | ✅ Excellent lines |
| `infrastructure/.../ActorContextRequestAttributeFilter.java` | 100% (10/10) | 83.3% (5/6) | One branch | ✅ Excellent |
| `infrastructure/.../GlobalExceptionHandler.java` | 60.0% (36/60) | 71.4% (5/7) | 24 whole-file lines | ⚠️ Low |

The configured threshold is 0, so coverage is informational. The new production clock factory line is not executed because the focused wiring harness overrides that bean with a fixed clock.

## Assertion Quality

No tautologies, ghost loops, assertions without production calls, or mock-to-assertion imbalance were found in the three task-created test files.

| File | Assertion | Issue | Severity |
|---|---|---|---|
| `AgentConversationWiringTest.java` | Five `isNotNull()` checks | Type-only checks do not prove concrete service type or singleton count for durable-memory input use cases. | WARNING |
| `AgentConversationWiringTest.java` | Private-field reflection | Couples the test to service field names; behavior/constructor-level proof would be less implementation-sensitive. | WARNING |
| `AgentConversationIT.java` | `lastHistorySize() <= 20` | The test does not seed more than 20 prior messages; truncation is proved only by companion layers. | WARNING |
| `AgentConversationIT.java` | `lastMemories().isNotEmpty()` | Uses a constant test memory port rather than the production persistence adapter. | WARNING |

**Assertion quality**: 0 CRITICAL, 4 WARNING.

## Review Workload Audit

Current working-tree measurement uses Git numstat semantics for tracked and untracked files and excludes this generated verification report.

| Boundary | Additions | Deletions | A+D | Assessment |
|---|---:|---:|---:|---|
| Historical 4.x child slice | — | — | 800 reported by prior apply/verification evidence | At the 800-line cap; reviewable with no margin. |
| Current measurable 5.x files | 599 | 2 | 601 | Under 800 by 199 lines; reviewable as an autonomous child. |
| Cumulative dirty worktree (4.x + 5.x + task checkboxes) | 1,370 | 31 | 1,401 | Not reviewable as one 800-line child diff. |

The present 5.x measurement is `WiringConfig` +159, `AgentConversationWiringTest` +438, and the two 5.x checkbox flips +2/-2. This does not match the launch note's 573 A+D or Engram's 578 A+D because the current untracked wiring test is 438 Git lines, not the previously recorded 415. The intended feature-branch-chain boundary remains reviewable **only if** 4.x and 5.x are separated so the later child targets the immediate predecessor and exposes the 601-line incremental diff. With both uncommitted slices coexisting, the current 1,401-line cumulative diff does not itself preserve a clean child-review boundary.

## Quality Metrics

**Linter**: ➖ Not available.
**Type checker**: ✅ `javac --release 21` passed through the six-module package build.
**Coverage threshold**: ✅ Configured threshold 0; informational warnings above.

## Issues Found

### CRITICAL

1. **Mandatory broad gate is non-zero.** `.\mvnw.cmd verify` exits 1 with 31 Infrastructure test-context errors. Causality is isolated to missing baseline test beans (`WaApiKeyFilter` and `FindBotByTokenUseCase`), not a focused agent regression, but policy still blocks PASS.
2. **CRM permission/ownership denial is untested and the deal tool cannot propagate actor authorization.** No passing test covers an unauthorized actor; `update_deal_stage` validates the actor context but calls an actor-free mutation contract.
3. **CRM write replay is not connected to production tools.** The ledger adapter passes its own H2 tests and is wired, but no `SpringAiCrmTools` method consumes its ports/action identity. Current create-contact and deal-note paths can repeat effects, so the current idempotent-write scenario has no covering production-path test.
4. **Ambiguous durable-memory update behavior is untested.** Stable reconstruction order passes, but no runtime test proves that an ambiguous update supersedes nothing.

### WARNING

1. The final H2 ingress test substitutes constant memory and completion seams; production Boot composition is proved only through companion layered tests, not one complete context.
2. The current 5.x measurable workload is 601 A+D, not the recorded 573/578; the discrepancy should be reconciled before review metadata is published.
3. Both slices coexist as a 1,401 A+D dirty diff. The feature-branch-chain is reviewable only after preserving a clean 4.x → 5.x incremental boundary.
4. Assertion-quality limitations and whole-file `GlobalExceptionHandler` coverage are documented above.
5. `agentToolActionClock()` is a new production bean line not executed by the wiring test because the harness overrides it.

### SUGGESTION

1. Add a future Boot/H2 no-network test that uses the real durable-memory adapter and asserts the concrete/singleton `ChatCompletionPort` and durable-memory input-service types. This is not a substitute for fixing the CRITICAL current-spec gaps.

## Final Verdict

**FAIL**

Tasks 4.1–5.2 and their focused runtime suites are complete, but Strict-TDD verification cannot pass with a non-zero configured broad gate or current-spec scenarios lacking passing behavioral coverage.
