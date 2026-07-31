# Design: Pipely Agent Conversation Memory — Restored Delivery

## Technical Approach

Deliver two feature-branch-chain children. A3 replaces the handwritten callback registry with shared Spring AI 2.0 annotated tools and per-request trusted actor context. Schemas come from actual method parameters, not internal records created after dispatch. The final PR adds one authenticated interaction over the existing turn and memory foundation.

## Architecture Decisions

| Decision | Choice and rationale |
|---|---|
| Tool contract | `@Tool` names methods; `@ToolParam` stays on actual primitive/UUID parameters, which define Spring AI's top-level schema and dispatch. Delete `FindContactsInput`, `CreateContactInput`, and `UpdateDealStageInput`; they are not request DTOs or schema sources. |
| Registration | Boot composes one stateless `SpringAiCrmTools`; `AgentConfig` registers it once with `defaultTools(tools)`. Delete the registry and `SpringAiCrmToolsBinder`. |
| Identity | `SpringAiChatCompletionAdapter` sends the JWT-derived CRM UUID through `.toolContext(Map.of("actorUsuarioId", actorUsuarioId))`. Tools require it from `ToolContext`; missing, invalid, or wrong-type context fails closed. Identity is never a model argument or fallback. |
| Default preservation | Do not call request `.tools(...)`, because Spring AI 2.0 would replace the three builder defaults. Only trusted tool context varies per request. |
| Validation and mapping | Each tool passes its dispatched parameters plus trusted actor directly to `CrmToolMapper`. Mapper entry points normalize strings; require company, name, relationship state, deal id, and status; validate known `EstadoRelacion`; allow only `GANADO`/`PERDIDO`; require normalized nonblank `motivo` for `PERDIDO`; and force find cap 20. They create existing Application commands or typed deal-stage arguments. Existing command/domain checks remain defense in depth. Mapper output methods retain the three bounded DTO records. No repository or new Application contract is introduced. |
| Tool failure boundary | The three `@Tool` methods contain no local `try/catch`. Trusted-actor checks and mapper validation still fail before use-case execution; validation, mapping, serialization, and use-case exceptions otherwise propagate naturally into Spring AI's tool execution mechanism and its `ToolExecutionException` handling. A custom exception policy is not part of A3. |
| REST | `POST /api/agent/messages` accepts `{message,idempotencyKey}`, derives owner/actor, uses bounded history, and returns `{content}`. Internal handles, completion, regeneration, and tools remain private. |
| Composition | `WiringConfig` composes tools and completion adapter from existing use cases, `ObjectMapper`, and `ChatClient`, preserving `boot → infrastructure → application → domain`. Existing turn and durable-memory services/adapters are reused. |

**Security invariant:** only the authenticated request actor may drive tool execution. It is server-attached per request, never stored on the shared bean, model-controlled, or exposed through schemas, prompts, outputs, or default observability.

## Two-Slice File Plan

### A3 — Annotation-driven tools

- Delete `SpringAiCrmToolRegistry.java`, `SpringAiCrmToolsBinder.java`, and `tool/dto/input/{FindContactsInput,CreateContactInput,UpdateDealStageInput}.java` under `infrastructure/src/main/java/com/ar/crm2/adapter/out/ai/`.
- Modify `tool/{SpringAiCrmTools,CrmToolMapper}.java`: annotate real method parameters, resolve actor context, validate/map raw values directly to existing commands, and retain bounded `tool/dto/output/{FindContactsOutput,CreateContactOutput,UpdateDealStageOutput}.java` projections. Remove all local `try/catch` wrappers from the three tool methods; invalid actor or input still stops before use-case execution, while other failures propagate to Spring AI unchanged.
- Modify `SpringAiChatCompletionAdapter.java` to set `.toolContext(...)` without `.tools(...)`, preserving history, durable memories, request-level completion behavior, and final content.
- Modify `boot/src/main/java/com/ar/crm2/config/WiringConfig.java`; preserve `AgentConfig.java` `defaultTools(tools)`, provider configuration, and actor propagation.
- Update focused tool, mapper, adapter, and Boot tests. Approved cleanup: Lombok `@RequiredArgsConstructor` on both infrastructure classes and a normal `Contacto` import.

`CompleteUserTurnService → ChatCompletionPort(actor) → ChatClient default tools + request ToolContext(actor) → shared SpringAiCrmTools → mapper → existing use case → safe result → final content`

### Final PR — Conversational REST ingress

Create `AgentController.java`, request/response DTOs, and `AgentRestMapper.java` under `infrastructure/.../adapter/in/rest/`, with focused tests. Modify `WiringConfig.java` and add `AgentConversationWiringTest.java`.

`JWT → ActorContext → AgentController → CreateUserTurnService → AgentTurnAdapter → CompleteUserTurnService → visible history + DurableMemoryPersistenceAdapter → ChatCompletionPort + default tools/tool context → persist ASSISTANT → content`

## Testing Strategy

- A3 callback tests verify three names, actual parameter schemas, identity exclusion, actor isolation, bounded outputs, and natural failure propagation through Spring AI's `ToolExecutionException` boundary. Tests assert invalid actor context and mapper validation prevent use-case calls; they do not assert local redaction or replacement exceptions. Mapper tests cover every moved validation, normalization, trusted-actor command mapping, cap 20, stage arguments, and output projection. Adapter/Boot tests prove tool context propagation and shared defaults.
- Final MVC tests cover authentication, validation, actor derivation, idempotent retry, and absent internal routes. A no-network H2 test proves history, durable memory, three tools, and single final persistence.

## Review Forecast / Rollout

| Slice | Forecast | Review boundary |
|---|---:|---|
| A3 | 700–800 lines target | Replace rejected registry/tests and redundant inputs. |
| Final REST | 450–700 lines | Ingress, focused tests, minimal wiring. |

No migration required. Each child targets its immediate predecessor and keeps tests with behavior. A3 above 800 lines remains a declared review risk.

## Deferred Future Hardening

Not fixed or blocking: custom sanitization, model-facing error conversion, or a global `ToolExecutionExceptionProcessor` policy; `TrustedAgentAction`/new per-tool ledger; deeper `CambiarEstadoTratoUseCase` authorization; advanced write idempotency/concurrency convergence; qualifier competition test; historical Strict-TDD reconstruction; unrelated 31 Infrastructure and 6 Boot baseline context failures unless final ingress directly affects those contexts.

## Risks / Open Questions

Existing write semantics remain until the deferred hardening. Until an exception policy is designed, failure presentation follows Spring AI's configured processor and may expose exception messages to the model or rethrow them; A3 makes no local redaction guarantee. Current A3 must be compressed substantially to meet budget. Open questions: none.
