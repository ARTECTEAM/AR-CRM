# Design: Pipely Agent Conversation Memory — V1 Correctness Correction

## Technical Approach

Preserve `POST /api/agent/messages`, visible history, durable-memory recall, Spring AI completion, and shared DefaultTools. Correct only writes: the completion adapter adds trusted owner and turn beside the actor UUID in request `ToolContext`; tools map model arguments but delegate both writes to a new Application orchestrator. Ambiguous durable-memory update/supersession remains outside v1.

## Architecture Decisions

| Decision | Choice and rationale |
|---|---|
| Trusted action scope | Use server-derived `(AgentOwnerId, TurnId, AgentToolName)` as the v1 slot; actor UUID and normalized arguments form its canonical payload. All trusted values come from `ChatCompletionPort` → `ToolContext`, stay outside schemas, and fail closed. Annotated Spring AI 2.0 tools cannot read the provider call id, so v1 allows one distinct invocation of each write tool per turn; another payload is rejected. |
| Deal authorization | No safe capability exists: `CambiarEstadoTratoUseCase` is actor-free and `TratoController` supplies no actor. The new Application service loads `Trato` and requires `responsableId == actorUsuarioId` before save/note. Strict ownership is safer than invented role semantics; REST stays unchanged. |
| Ledger integration | Reuse `AgentToolAction`, its three ports, and adapter; add one atomic-execution port and a unique `(owner_id, turn_id, tool_name)` slot. Application compares canonical payloads, replays `COMPLETED`, rejects mismatch, or executes `PENDING`. |
| Atomicity | Claim commits briefly. Execution locks the row and atomically authorizes, writes CRM state/note, and completes the ledger. Failure rolls back effects/completion, leaving `PENDING`; concurrent retries wait and replay. |
| Canonical replay | The service returns the stored resource. Tools rebuild the bounded output from the matched normalized command and resource id, without another CRM call. |

## Data Flow

```text
JWT ActorContext → CompleteUserTurnService → ChatCompletionPort
  → ToolContext(owner, actor UUID, turn) → SpringAiCrmTools
  → AgentCrmWriteUseCase → claim canonical action
  → lock action → authorize → CRM save/note → complete ledger → commit
                         ↘ completed replay: prior bounded outcome, no effect
```

## File Changes

| File | Action | Description |
|---|---|---|
| `application/src/main/java/com/ar/crm2/application/agent/tool/command/AgentCrmWriteCommand.java` | Create | Sealed trusted create/stage commands. |
| `application/src/main/java/com/ar/crm2/application/agent/tool/port/in/AgentCrmWriteUseCase.java` | Create | Write orchestration boundary. |
| `application/src/main/java/com/ar/crm2/application/agent/tool/port/out/ExecuteAgentToolActionAtomicallyPort.java` | Create | Lock-held effect contract. |
| `application/src/main/java/com/ar/crm2/application/agent/tool/service/AgentCrmWriteService.java` | Create | Canonicalization, replay, mismatch, and deal ownership policy. |
| `infrastructure/src/main/java/com/ar/crm2/adapter/out/ai/SpringAiChatCompletionAdapter.java` | Modify | Attach trusted owner and turn beside actor. |
| `infrastructure/src/main/java/com/ar/crm2/adapter/out/ai/tool/{SpringAiCrmTools,CrmToolMapper}.java` | Modify | Require trusted scope, delegate writes to Application, and render canonical replay. |
| `infrastructure/src/main/java/com/ar/crm2/adapter/out/persistence/agent/tool/{AgentToolActionEntity,AgentToolActionRepository,AgentToolActionPersistenceAdapter}.java` | Modify | Unique action slot plus lock-held atomic execution. |
| `boot/src/main/java/com/ar/crm2/config/WiringConfig.java` | Modify | Compose the new Application service/port; keep one shared tools bean. |
| `application/src/test/java/com/ar/crm2/application/agent/tool/service/AgentCrmWriteServiceTest.java` | Create | Application RED/GREEN policy tests. |
| Existing `SpringAiCrmToolsTest`, `SpringAiChatCompletionAdapterTest`, `AgentToolActionPersistenceAdapterTest`, `AgentConfigTest`, and `AgentConversationWiringTest` | Modify | Adapter, transaction, and composition proofs. |

## Interfaces / Contracts

`AgentCrmWriteUseCase.execute(AgentCrmWriteCommand)` receives trusted scope plus normalized data. `ExecuteAgentToolActionAtomicallyPort.execute(claim, effect)` returns the immutable completed action; `effect` runs only while canonical `PENDING` is locked. Neither contract is model-visible or controller-owned.

## Testing Strategy

| Layer | RED/GREEN proof |
|---|---|
| Application unit | Owner succeeds; unauthorized actor causes no deal save/note; completed replay skips effects; mismatched actor/payload fails first. |
| Infrastructure H2 | Context stays outside schemas; both writes replay identical JSON; concurrent/sequential retries create one contact or one stage event/note; injected failures roll back effects and completion. |
| Boot/component | Service and atomic port are unique and wired into unchanged shared DefaultTools. No network E2E. |

## Migration / Rollout and Review Workload Forecast

The pre-release ledger must be empty before adding the unique slot constraint; no CRM business-data migration is required. Roll back by disabling ingress/tool registration.

One child is not realistic: forecast **950–1,250 A+D**. Use the approved feature-branch chain: **C1 authorization + trusted owner/turn propagation (350–500)**, then **C2 atomic ledger integration for both writes (600–750)**. Each child carries its RED/GREEN tests; C2 must be split again before apply if it forecasts above 800.

## Open Questions

None. General role-based deal permission and ambiguous durable-memory supersession are v2 work, not implementation tasks for this correction.
