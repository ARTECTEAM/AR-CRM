# Tasks: Pipely Agent Conversation Memory — Restored Delivery

## Review Workload Forecast

| Field | Value |
|---|---|
| Review budget | 800 changed lines (additions + deletions) |
| Estimated review cleanup | 250–400 changed lines (additions + deletions) |
| 800-line budget risk | Low |
| 400-line budget risk | Medium |
| Chained PRs recommended | No additional PR; keep one compact A3 cleanup unit |
| Suggested split | A3 review cleanup → pending Final conversational PR |
| Delivery strategy | auto-forecast |
| Chain strategy | feature-branch-chain (approved) |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: feature-branch-chain
400-line budget risk: Medium
800-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Boundary |
|---|---|---|
| **A3 review cleanup — FIRST** | Remove redundant inputs and centralize raw-value mapping | 250–400; base=feature/tracker; focused Infrastructure/Boot tests |
| Final conversational PR | Authenticated ChatGPT-style ingress | Pending; base=A3 cleanup branch; unchanged scope |

## Completed History

- [x] 0.1 Turn completion, idempotent visible history, bounded reconstruction, and durable owner-memory foundations exist.
- [x] 0.2 `ChatCompletionPort`, Spring AI configuration, trusted actor propagation, and the initial annotation-driven A3 attempt exist.
- [x] 0.3 Shared default tools and request `ToolContext` replaced the binder-based attempt; maintainer review reopened the cleanup below.

## A3 Review Cleanup — RED (Strict TDD)

- [x] 1.1 **RED:** Rewrite only stale cases in `infrastructure/src/test/java/com/ar/crm2/adapter/out/ai/tool/SpringAiCrmToolsTest.java` and `CrmToolMapperTest.java` for primitive/UUID schemas, raw mapping, fail-closed validation, bounded outputs, and Spring AI exception propagation.
- [x] 1.2 **RED:** Update constructor/context expectations in `infrastructure/src/test/java/com/ar/crm2/adapter/out/ai/SpringAiChatCompletionAdapterTest.java`, `boot/src/test/java/com/ar/crm2/config/AgentConfigTest.java`, and `AgentConfigOpenAiWiringTest.java`; retain actor isolation and shared defaults.

## A3 Review Cleanup — GREEN/REFACTOR

- [x] 2.1 Delete `infrastructure/src/main/java/com/ar/crm2/adapter/out/ai/tool/dto/input/{FindContactsInput,CreateContactInput,UpdateDealStageInput}.java`; update `infrastructure/src/main/java/com/ar/crm2/adapter/out/ai/tool/SpringAiCrmTools.java` with `@ToolParam` only on dispatched primitive/UUID parameters, imported `Contacto`, no local catches, and bounded outputs.
- [x] 2.2 Move normalization, validation, command creation, and typed stage arguments into `infrastructure/src/main/java/com/ar/crm2/adapter/out/ai/tool/CrmToolMapper.java`; pass raw values plus trusted actor directly.
- [x] 2.3 Add Lombok `@RequiredArgsConstructor` and remove manual constructors in `infrastructure/src/main/java/com/ar/crm2/adapter/out/ai/tool/SpringAiCrmTools.java` and `infrastructure/src/main/java/com/ar/crm2/adapter/out/ai/SpringAiChatCompletionAdapter.java`; preserve fail-closed actor checks and request tool context.

## A3 Review Cleanup — Safety Net

- [x] 3.1 Run focused Infrastructure and Boot tests for schemas, mapper validation, actor isolation, exception propagation, shared defaults, and constructor wiring; confirm deleted DTO/catch references are absent.
- [x] 3.2 Measure additions + deletions for this cleanup; keep it at or below 800 lines and review a split if it exceeds the 400-line forecast.

## Final Conversational PR — Pending (Strict TDD)

- [ ] 4.1 **RED:** Add `infrastructure/src/test/java/com/ar/crm2/adapter/in/rest/{AgentControllerMvcTest,AgentConversationIT}.java` for authentication, validation, trusted identity, retries, absent internal routes, and no-network persisted final response.
- [ ] 4.2 **GREEN:** Create `infrastructure/src/main/java/com/ar/crm2/adapter/in/rest/{AgentController,AgentRestMapper}.java` and request/response DTOs; expose only `POST /api/agent/messages` through existing turn services.
- [ ] 5.1 **RED:** Add `boot/src/test/java/com/ar/crm2/config/AgentConversationWiringTest.java` for existing turn/memory adapters, services, and ports.
- [ ] 5.2 **GREEN/REFACTOR:** Add only required final beans to `boot/src/main/java/com/ar/crm2/config/WiringConfig.java`; verify focused MVC/H2 suites.

## Explicit Deferrals

`TrustedAgentAction`/new ledger; deeper deal authorization; advanced write convergence; qualifier competition; historical TDD reconstruction; unrelated baseline context failures unless final wiring directly touches them.
