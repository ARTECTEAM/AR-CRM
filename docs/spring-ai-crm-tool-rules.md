# Spring AI CRM Tool Rules

This document is the canonical rulebook for every Spring AI 2.0 CRM
tool exposed by `com.ar.crm2.adapter.out.ai.tool.SpringAiCrmTools`. It
is normative: any new CRM tool must follow every rule below, and any
existing tool that drifts from these rules is a regression to fix.

## Outcome

A new allowlisted CRM tool must be:

1. **Thin** — a mapper-and-delegate adapter over an existing Application
   use case. No business logic, no repository access, no authorization
   invention.
2. **Discoverable** — registered once on the shared
   `SpringAiCrmTools` bean via `defaultTools(...)` on the configured
   `ChatClient` builder. No per-invocation `.tools(...)` calls (they
   would replace builder defaults in Spring AI 2.0).
3. **Bounded** — the model's schema and the model's output strip every
   internal handle, identity, or sensitive business field.

## Quick path to add a new tool

1. Decide which existing Application use case owns the capability. If
   none exists, write the use case first in `application/.../<aggregate>/port/in/`
   and its `service/` implementation; do not skip straight to the tool.
2. Add a `@Tool(name = "<snake_case>", description = "...")` method on
   `SpringAiCrmTools` that maps raw parameters plus trusted context to
   the existing Application command and delegates to the use case.
3. Add (or reuse) a bounded output record in
   `infrastructure/.../adapter/out/ai/tool/dto/output/`.
4. Add a `CrmToolMapper` method that converts raw values to the
   Application command and another that projects the domain entity to
   the bounded output.
5. Update the production default-system template in
   `boot/.../config/AgentConfig.java` to advertise the new tool name.
6. Add focused tests (next section).
7. Update this file's tool inventory and any directly relevant OpenSpec
   spec under `openspec/changes/<change>/specs/agent-crm-tools/spec.md`.

## Naming and descriptions

| Rule | Why |
|------|-----|
| `@Tool(name = ...)` MUST be `snake_case` (e.g. `edit_trato`). | Spring AI 2.0 generates the JSON schema from the Java method unless `name` overrides; snake_case matches the existing CRM convention and the model prompt. |
| The Java method MUST follow Java conventions (e.g. `editTrato`). | Discoverability and code style consistency. |
| `@Tool(description = ...)` MUST explain **when** to call the tool and **what** it does, in one or two sentences, written for the model. | The description is the model's primary selection signal. Vague descriptions cause hallucinated invocations. |
| The description MUST state that the authenticated actor identity is trusted and is NOT a model-visible argument. | Prevents prompt-injection attempts to override identity. |
| The description MUST list every field that is intentionally NOT editable (e.g. `edit_trato` does not change stage). | Prevents the model from "trying" to mutate fields the use case preserves. |

## Parameters — required/optional and trust

| Rule | Why |
|------|-----|
| Every `@ToolParam(required = false, ...)` MUST match the underlying use-case contract: optional means the use case accepts null and produces a sensible result. | False optionality causes silent contract mismatches. |
| Every model-supplied identity MUST be marked `required = false` and rejected at the trust boundary (mapper or `requireActor`). | Identity is resolved from `ToolContext`, not from the model. |
| `ToolContext` (and therefore the trusted actor) MUST reach the tool method so identity stays out of the JSON schema. | `JsonSchemaGenerator` excludes `ToolContext` automatically. |
| The trusted actor MUST be read with `requireActor(toolContext)` (UUID, present, non-null) before any use case invocation. | Defense-in-depth: missing or wrong-type actor fails closed. |
| `responsableId` (or any business "responsible user" field) MUST be a model-visible parameter; it is NOT a stand-in for the authenticated actor. | The two identities are different concepts. Misusing one as the other breaks audit and authorization. |

## Thin adapter rule and canonical use-case reuse

| Rule | Why |
|------|-----|
| The tool MUST NOT call a repository, persist data, or implement business rules. | Domain rules live in `domain` or are orchestrated in `application`. Tools are infrastructure. |
| The tool MUST delegate to an existing Application use case under its `port/in` interface. | Preserves the "controllers AND tools share canonical business use cases" rule and avoids agent-specific duplicates. |
| The tool MUST NOT introduce an agent-specific use case or service for capability that already exists. | Same reason: the canonical use case is the source of truth. |
| If the canonical use case is missing a needed field or behavior, fix the use case first, then re-expose it. | The tool surface is derived from the canonical contract, not the other way around. |

## Validation and error behavior

| Rule | Why |
|------|-----|
| All input validation MUST happen in the mapper (`CrmToolMapper`) before the use case runs. | Spring AI wraps mapper exceptions in `ToolExecutionException` with the original `IllegalArgumentException` as the cause; local sanitization hides real errors. |
| Validation messages MUST be specific and machine-readable (e.g. `"edit_trato requires responsableId"`). | The model uses the message to recover and the test suite uses it to assert behavior. |
| The tool MUST NOT catch and rewrap exceptions from the use case. | Spring AI's natural wrapper preserves the cause type and message; rewriting loses information. |
| Required fields MUST be validated by both the `@ToolParam(required = true)` annotation (for the model) and the mapper (for the trust boundary). | Schema-required fields are advisory; the trust boundary is authoritative. |

## Structured, bounded, non-sensitive outputs

| Rule | Why |
|------|-----|
| Every tool MUST return a JSON-serialized record via `ObjectMapper.writeValueAsString`. | Stable contract for the model and the tests. |
| Output records MUST live in `infrastructure/.../adapter/out/ai/tool/dto/output/`. | One location, one naming convention. |
| Outputs MUST NOT include: `creadoPor`, `actualizadoEn`, persistence timestamps, raw SQL, stack traces, JWTs, internal handles, cross-owner data. | Prevents information leakage through the model surface. |
| Output fields MUST match the editable fields the use case actually persists. | Surfaces stage/loss-reason only when the use case changed them. |
| Output records MUST carry `@JsonProperty` annotations matching their JSON keys. | Stable serialization regardless of record component renames. |

## Write-tool authorization, idempotency, and audit

| Rule | Why |
|------|-----|
| Every write tool MUST require the authenticated actor via `ToolContext` before the use case runs. This validates trusted request context; it does not by itself prove target authorization or persisted audit. | Identity discipline without overstating downstream enforcement. |
| Every write tool MUST delegate to a use case that is the source of truth for authorization semantics — do not re-implement ownership/role checks in the tool. | Avoids drift between tool and REST surfaces. |
| **DEVELOPMENT-ONLY TECHNICAL DEBT:** `edit_trato` validates that trusted actor context exists, then delegates to `EditTratoUseCase`, which does NOT receive or check that actor. Therefore the current backend path performs no actor-aware target authorization for this mutation. This gap is accepted temporarily so the maintainer can observe how the LLM uses the tool. Production safety requires adding `actorUsuarioId` to `EditTratoCommand` (or a parallel authorization adapter) and enforcing it in the use case. Do NOT close this gap in tooling code; redesign the Application authorization model in a dedicated task. | Honest record of the gap; the rules remain normative for production. |
| Idempotency for write tools MUST live in the Application layer (e.g. via the agent tool-action ledger), not in the tool. | The tool should stay a thin mapper. |
| Write effects MUST be auditable through the existing Application logging or the durable agent-tool-action ledger; the tool MUST NOT add its own audit logging. | One audit story, owned by Application. |

## Focused contract and wiring tests

For every new (or modified) tool, add or update the following tests:

| Test class | Location | Must cover |
|------------|----------|------------|
| `SpringAiCrmToolsTest` | `infrastructure/.../test/.../adapter/out/ai/tool/` | Allowlist exact six names; each tool's discovery carries real annotation metadata; the generated JSON schema requires exactly the documented fields; schemas never expose actor/owner/turn/handle; mapper validation surfaces through `ToolExecutionException` with the original `IllegalArgumentException` cause; use-case failure propagates unchanged. |
| `CrmToolMapperTest` | `infrastructure/.../test/.../adapter/out/ai/tool/` | Mapper accepts valid required + optional inputs; rejects null/blank required inputs with the documented message; rejects unknown enum names; maps domain entity to bounded output. |
| `AgentConfigTest` | `boot/.../test/.../config/` | Production default-system template advertises every allowlisted tool by name. |
| `AgentConfigOpenAiWiringTest` | `boot/.../test/.../config/` | Same — through the configured `ChatClient` round-trip. |
| `AgentConversationWiringTest` | `boot/.../test/.../config/` | The canonical use case backing each write tool is wired exactly once. |

When you remove a tool, remove its wiring bean, its mapper methods,
its output DTO, and every test entry that references it. Do NOT leave
half-removed artifacts.

## Template checklist for adding a future tool

Copy and complete this checklist when adding a new tool.

```markdown
### Tool: `<name>`

- [ ] Existing Application use case: `<fully.qualified.UseCase>` accepts `<command>`
- [ ] `SpringAiCrmTools` method: `<javaMethodName>` with `@Tool(name = "<snake_name>", description = "...")`
- [ ] `@ToolParam` set on every parameter; required fields documented in the description
- [ ] Output record created under `dto/output/`
- [ ] `CrmToolMapper.to<Name>Command(...)` validates required fields, parses enums, and rejects unknowns
- [ ] `CrmToolMapper.to<Name>Output(...)` projects domain entity to the bounded output record
- [ ] `WiringConfig` injects the canonical use case into the `springAiCrmTools` bean
- [ ] `AgentConfig.DEFAULT_SYSTEM_TEMPLATE` advertises the new tool by name
- [ ] Tests added in `SpringAiCrmToolsTest`, `CrmToolMapperTest`, `AgentConfigTest`, `AgentConfigOpenAiWiringTest`, `AgentConversationWiringTest`
- [ ] Allowlist test still asserts exactly the right set of tool names
- [ ] No raw `ToolContext` field, actor id, or owner subject leaks into the generated schema
- [ ] No business logic, repository access, or authorization check inside the tool method
- [ ] Authorization gap (if any) recorded as **DEVELOPMENT-ONLY TECHNICAL DEBT** in this file
- [ ] OpenSpec `agent-crm-tools/spec.md` updated with the tool's contract and a scenario
```

## Tool inventory (current)

| Tool | Java method | Use case | Notes |
|------|-------------|----------|-------|
| `find_contacts` | `findContacts` | `GetAllContactosUseCase` | Read-only, hard cap 20, trusted actor injected. |
| `create_contact` | `createContact` | `CreateContactoUseCase` | Required: `empresaId`, `nombre`, `estadoRelacion`. Trusted actor becomes `creadoPor`. |
| `edit_contact` | `editContact` | `EditContactoUseCase` | Required: `id`, `nombre`, `estadoRelacion`. Optional: `correo`, `responsableId`, `telefono`, `cargo`, `comoNosConocio`. Preserves `empresaId` and `creadoPor`. |
| `create_company` | `createCompany` | `CreateEmpresaUseCase` | Required: `nombre`. Optional: `sector`, `telefono`, `paginaWeb`, `facebook`, `instagram`, `twitter`, `estadoRelacion`, `responsableId`, `notas`. Trusted actor becomes `creadoPor`. |
| `edit_company` | `editCompany` | `EditEmpresaUseCase` | Required: `id`, `nombre`. Optional: `sector`, `telefono`, `paginaWeb`, `facebook`, `instagram`, `twitter`, `estadoRelacion`, `responsableId`, `notas`. Preserves `creadoPor`. |
| `edit_trato` | `editTrato` | `EditTratoUseCase` | Required: `id`, `responsableId`, `nombre`. Optional: `valorEstimado`, `probabilidad`, `fechaCierreEsperada`, `tipoContrato`. Preserves non-editable deal state. **DEVELOPMENT-ONLY TECHNICAL DEBT:** trusted actor presence is validated at the tool boundary, but `EditTratoUseCase` does not receive the actor or enforce actor-aware target authorization. |

Company deletion is intentionally NOT exposed: no `delete_company` tool exists, and none must be added without re-opening the canonical authorization design.
