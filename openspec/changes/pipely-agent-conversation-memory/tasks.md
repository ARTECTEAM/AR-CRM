# Tasks: Pipely Agent Conversation Memory — V1 Correctness

## Review Workload Forecast

| Field | Value |
|---|---|
| Corrective estimate | 950–1,250 A+D |
| Child budget | 800 A+D |
| 400 / 800 risk | High / Medium |
| Split | C1 350–500 → C2 600–750 |
| Delivery / chain | auto-forecast / feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High
800-line budget risk: Medium

### Work Units and Boundaries

| Unit | Boundary | Finish / verification / rollback |
|---|---|---|
| C1 | Base=feature/tracker after preserving 4.x→5.x; protect `application/src/main/java/com/ar/crm2/application/agent/tool/`, `infrastructure/src/main/java/com/ar/crm2/adapter/out/ai/`, `boot/src/main/java/com/ar/crm2/config/WiringConfig.java` | Trusted context plus authorization; focused tests; revert C1 only |
| C2 | Base=C1; protect `application/src/main/java/com/ar/crm2/application/agent/tool/` and `infrastructure/src/main/java/com/ar/crm2/adapter/out/persistence/agent/tool/` | Atomic replay for both writes; focused/broad gates; revert C2 only |

The existing 4.x/5.x dirty slices (~1,401 A+D) MUST NOT enter C1/C2 or be staged/committed here. Preserve their parent boundary before apply. Baseline Security/Usuario/Etiqueta/Tablero/Ficha context repairs remain separate; they may keep final `mvn verify` non-zero and must be reported independently.

## Completed Delivery (Preserved)

- [x] 1.1 `infrastructure/src/test/java/com/ar/crm2/adapter/out/ai/tool/`: RED schema/mapping tests.
- [x] 1.2 `infrastructure/src/test/` and `boot/src/test/`: RED trusted-actor/default tests.
- [x] 2.1 `infrastructure/src/main/java/com/ar/crm2/adapter/out/ai/tool/`: primitive inputs; DTOs removed.
- [x] 2.2 `CrmToolMapper.java`: centralized validation/mapping.
- [x] 2.3 `infrastructure/src/main/java/com/ar/crm2/adapter/out/ai/`: constructor injection; fail-closed actor checks.
- [x] 3.1 `infrastructure/` and `boot/`: focused safety tests passed.
- [x] 3.2 `tasks.md`: cleanup A+D measured.
- [x] 4.1 `infrastructure/src/test/java/com/ar/crm2/adapter/in/rest/`: RED MVC/integration tests.
- [x] 4.2 `infrastructure/src/main/java/com/ar/crm2/adapter/in/rest/`: sole conversational ingress.
- [x] 5.1 `boot/src/test/java/com/ar/crm2/config/AgentConversationWiringTest.java`: RED wiring test.
- [x] 5.2 `boot/src/main/java/com/ar/crm2/config/WiringConfig.java`: GREEN/REFACTOR composition.

## C1 — Trusted Context and Deal Authorization (RED → GREEN → REFACTOR)

- [x] 6.1 **RED:** Create `application/src/test/java/com/ar/crm2/application/agent/tool/service/AgentCrmWriteServiceTest.java`; extend `infrastructure/src/test/java/com/ar/crm2/adapter/out/ai/{SpringAiChatCompletionAdapterTest,tool/SpringAiCrmToolsTest}.java` for trusted owner/actor/turn, pre-effect ownership denial, and no model-visible identity.
- [x] 6.2 **GREEN:** Create `application/src/main/java/com/ar/crm2/application/agent/tool/{command/AgentCrmWriteCommand,port/in/AgentCrmWriteUseCase,service/AgentCrmWriteService}.java`; load `Trato` and require `responsableId == actorUsuarioId` before stage save/event/note.
- [x] 6.3 **GREEN:** Update `infrastructure/src/main/java/com/ar/crm2/adapter/out/ai/{SpringAiChatCompletionAdapter,tool/CrmToolMapper,tool/SpringAiCrmTools}.java` to carry trusted context through `ToolContext` and delegate stage writes through Application.
- [x] 6.4 **GREEN:** Update `boot/src/main/java/com/ar/crm2/config/WiringConfig.java` and focused Boot tests; retain one endpoint, exact three-tool allowlist, and unchanged find/create behavior.
- [x] 6.5 **REFACTOR:** Run focused suites for `application/src/main/java/com/ar/crm2/application/agent/tool/`, `infrastructure/src/main/java/com/ar/crm2/adapter/out/ai/`, and `boot/src/main/java/com/ar/crm2/config/WiringConfig.java`; prove denial has no effect and C1 stays ≤800 A+D.

## C2 — Atomic Write Idempotency (RED → GREEN → REFACTOR)

- [ ] 7.1 **RED:** Extend `application/src/test/java/com/ar/crm2/application/agent/tool/service/AgentCrmWriteServiceTest.java` and `infrastructure/src/test/java/com/ar/crm2/adapter/out/{persistence/agent/tool/AgentToolActionPersistenceAdapterTest,ai/tool/SpringAiCrmToolsTest}.java` for replay, mismatch, rollback, and zero duplicate effects.
- [ ] 7.2 **GREEN:** Complete `application/src/main/java/com/ar/crm2/application/agent/tool/{command,port/in,service}/` around trusted `(owner, turn, tool)` and actor-plus-normalized-arguments fingerprints; reuse claim/complete/replay, return stored resources, and reject mismatches.
- [ ] 7.3 **GREEN:** Create `application/src/main/java/com/ar/crm2/application/agent/tool/port/out/ExecuteAgentToolActionAtomicallyPort.java`; update `infrastructure/src/main/java/com/ar/crm2/adapter/out/persistence/agent/tool/{AgentToolActionEntity,AgentToolActionRepository,AgentToolActionPersistenceAdapter}.java` for unique slots, locks, atomic effect/completion, and retryable `PENDING` failures.
- [ ] 7.4 **GREEN:** Update `infrastructure/src/main/java/com/ar/crm2/adapter/out/ai/tool/{SpringAiCrmTools,CrmToolMapper}.java` so both writes use the ledger and canonical replay without another CRM call; preserve one distinct invocation per write tool per turn.
- [ ] 7.5 **GREEN:** Update `boot/src/main/java/com/ar/crm2/config/WiringConfig.java` and `boot/src/test/java/com/ar/crm2/config/{AgentConfigTest,AgentConversationWiringTest}.java` for unique service/atomic-port composition.
- [ ] 7.6 **REFACTOR:** Run focused suites for `application/src/test/java/com/ar/crm2/application/agent/tool/`, `infrastructure/src/test/java/com/ar/crm2/adapter/out/{ai,persistence/agent/tool}/`, and `boot/src/test/java/com/ar/crm2/config/`, then broad `mvn verify`; report baseline failures separately and confirm C2 stays ≤800 A+D.
