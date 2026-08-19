# Design: Pipely Agent Conversation Memory — Gated CRM Tool Catalog

## Catalog composition

`SpringAiCrmTools` owns the six default callbacks. `SpringAiDevelopmentCrmTools` owns the 15 tablero/columna/ficha callbacks. `AgentDevelopmentToolsConfig` creates the latter only when `crm2.agent.development-tools-enabled=true` (default `false`) and `AgentDevelopmentToolsEnvironmentGuard` confirms that exactly one Spring profile is active and it is `noauth` (local development) or `test`. No profile, `prod`, `production`, unknown profiles, and every mixed profile set—including `noauth,test`—reject flag `true` with one stable configuration error. `AgentConfig` registers and advertises the objects actually present: six callbacks when disabled, 21 only after the guard accepts the runtime.

This is registration enforcement, not prompt-only policy. No delete/remove callback is declared in either object.

## Trusted identity flow

```text
JWT ActorContext(usuarioId, optional superUsuarioId)
  -> AgentRestMapper
  -> CompleteUserTurnCommand
  -> CompleteUserTurnService
  -> ChatCompletionPort
  -> SpringAiChatCompletionAdapter ToolContext
  -> tool method (identity absent from model schema)
```

The two claims are never substituted for each other. `create_columna` maps the optional trusted super-user claim directly. `PREDETERMINADA` requires that claim; normal `PERSONALIZADA` creation sends `Optional.empty()`.

## Output and error boundaries

- Top-level lists use stable typed-ID ordering and hard cap 50.
- Nested board columns and card labels use stable typed-ID ordering and hard cap 25.
- Sentinel-backed `find_contacts` exposes `returned` plus truthful `truncated`; it does not claim an exact total beyond the query sentinel.
- Fully materialized development lists expose exact `total` plus `truncated`; nested fully materialized collections expose corresponding exact totals/truncation flags.
- Every output record list is null-normalized and defensively copied.
- Every CRM `@Tool` method returns its concrete bounded DTO/record directly. Spring AI 2.0's `DefaultToolCallResultConverter` owns Jackson serialization; custom formatting must use `@Tool(resultConverter = ...)`, never tool-local `ObjectMapper` calls.
- Plain bounded records use canonical component names as their model-visible JSON contract. A component rename is an explicit breaking tool-contract change and callback-level JSON contract tests must catch it. Use Jackson 3 `tools.jackson.annotation.JsonProperty` only when an intentionally required JSON key differs from the Java component name; never use Jackson 2 `com.fasterxml.jackson.annotation.JsonProperty` to control Spring AI 2.0 tool-result serialization.
- One auto-registered `ToolCallingAdvisor` owns the real loop through an explicit `ToolCallingManager` configured with `SafeToolExecutionExceptionProcessor`; no second tool advisor is registered.
- `SafeToolExecutionExceptionProcessor` returns a fixed validation code/message only for a `SafeToolValidationException` in the cause chain; all other failures become stable `TOOL_EXECUTION_FAILED` output without downstream text. Both records are serialized by Jackson rather than string-concatenating exception messages.
- `edit_ficha.etiquetaIds` is schema-required and is a complete replacement; explicit `[]` clears all labels.

## Known production blockers

The current tablero/columna/ficha list/get/edit/move use cases are globally scoped and do not persist/enforce actor or tenant ownership. `CreateTableroService` does not establish ownership. `edit_trato` also delegates to an actor-free use case.

The durable `AgentToolAction` model derives identity from owner, turn, operation name, and canonical arguments, but its claim/completion transactions are separate from CRM aggregate writes. It cannot guarantee exactly-once effects across a crash window. Integrating it superficially would create false assurance, so the 15 mutating development tools remain gated and idempotency is an explicit production blocker.

## Delivery state

Current branch: `feat/agent-tablero-columna-ficha-tools`. No commit or push is prepared by this corrective pass. The exact total diff after this pass is recorded in `tasks.md` and `verify-report.md`; exact per-slice counts remain unknown until staging proves them. Build a Feature Branch Chain in this dependency order, targeting at most 400 changed lines per child where technically feasible:

1. Trusted identity transport, including its command/service/adapter tests and identity documentation.
2. Safe default-tool execution loop, including the configured manager/processor, real-loop redaction tests, and error-policy documentation.
3. Bounded output contracts, including DTO/mapper tests and output-contract documentation.
4. Read-only development callbacks, with callback tests and documentation, still unregistered.
5. Tablero/columna mutations, with their tests and documentation, still unregistered.
6. Ficha mutations, including full-replacement `etiquetaIds`, with tests and documentation, still unregistered.
7. Environment-safe registration and catalog integration, with the exact-profile matrix, request/prompt synchronization tests, and deployment/catalog documentation.

Shared files require hunk-level staging or reconstruction onto each child branch. Every intermediate branch must compile and pass its focused tests. Documentation and verification evidence travel with the behavior each slice introduces; there is no detached optional documentation slice.
