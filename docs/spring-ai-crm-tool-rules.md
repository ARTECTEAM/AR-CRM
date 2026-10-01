# Spring AI CRM Tool Rules

This document is the canonical rulebook for the nine Spring AI CRM tool
groups in `com.ar.crm2.adapter.out.ai.tool`. The groups expose 50 callbacks:
49 controller-equivalent CRM operations plus the actor-scoped `find_contacts`.
Any new tool must follow these rules, and any existing tool that drifts is a
regression to fix.

## Outcome

A new allowlisted CRM tool must be:

1. **Thin** — a mapper-and-delegate adapter over an existing Application
   use case. No business logic, no repository access, no authorization
   invention.
2. **Discoverable** — the nine resource groups are registered via
   `defaultTools(...)` on the configured `ChatClient` builder. No per-invocation `.tools(...)` calls (they
   would replace builder defaults in Spring AI 2.0).
3. **Bounded** — the model's schema and the model's output strip every
   internal handle, identity, or sensitive business field.

## Quick path to add a new tool

1. Decide which existing Application use case owns the capability. If
   none exists, write the use case first in `application/.../<aggregate>/port/in/`
   and its `service/` implementation; do not skip straight to the tool.
2. Add a `@Tool(name = "<snake_case>", description = "...")` method to
   the controller-aligned `<Resource>Tools` class. Map inputs and trusted
   context to the canonical Application command and delegate to its use case.
3. Add (or reuse) a bounded output record in
   `infrastructure/.../adapter/out/ai/tool/dto/output/`.
4. Validate and map at the boundary that owns each invariant: mapper,
   tool trust boundary, or Application use case. Add a `CrmToolMapper`
   projection for bounded output where needed.
5. Update the production default-system template in
   `boot/.../config/AgentConfig.java` to advertise the new tool name.
6. Add focused tests (next section).
   Use the domain enum type directly for enum arguments and mapper inputs.
   Spring AI generates its allowed names; retain explicit required-null checks.
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

Enum arguments use domain types directly. Spring AI/Jackson owns deserialization:
unknown names are rejected before invocation; optional omitted/null inputs remain
null. Its default conversion also accepts numeric and numeric-string ordinals
and trims surrounding whitespace. The schema advertises enum names, not ordinals.
Required enums are checked for null by the mapper, and trusted-context checks run
on the converted enum, including ordinal inputs. Conversion failures use the
central generic redacted tool-error response; no custom coercion layer is added.

| Rule | Why |
|------|-----|
| Validation MUST occur at the mapper, tool boundary, or Application layer according to ownership of the invariant. | Parsing belongs in the mapper, trusted-context checks belong at the tool boundary, and business invariants remain in Application/domain. |
| Model-visible failures MUST use stable generic redacted codes/messages. Arbitrary downstream or validation exception messages MUST never be copied into tool results. | Exception text can contain SQL, identifiers, credentials, or unstable implementation detail. |
| Only deliberately classified safe-validation failures MAY select the stable validation code; their original exception text still MUST NOT become model visible. | Classification supports recovery without turning exception messages into an exfiltration channel. |
| The tool MUST NOT catch and rewrap arbitrary use-case exceptions; the configured centralized processor performs classification and redaction. | One error boundary prevents per-tool drift and leakage. |
| Required fields MUST be represented in `@ToolParam(required = true)` and validated again at the boundary that owns the invariant. | Schema-required fields are advisory; the server boundary is authoritative. |
| The trusted `superUsuarioId`/`PREDETERMINADA` check MAY run at the tool boundary because it verifies server-created identity context before delegation; the Application service MUST continue enforcing its own column-creation rule. | This is defense in depth at a trust boundary, not duplicated target authorization. |

## Structured, bounded, non-sensitive outputs

| Rule | Why |
|------|-----|
| Every `@Tool` method MUST return its concrete bounded DTO/record type. Spring AI 2.0's `DefaultToolCallResultConverter` owns Jackson serialization of that value for the model. Tool methods MUST NOT call `ObjectMapper` for result serialization. | One typed adapter contract and one framework-owned serialization boundary prevent double encoding and per-tool drift. |
| Custom result formatting MUST use `@Tool(resultConverter = ...)` with a dedicated `ToolCallResultConverter`, never ad hoc JSON calls inside the tool method. | Keeps conversion explicit, discoverable, and controlled by Spring AI's tool execution pipeline. |
| Output records MUST live in `infrastructure/.../adapter/out/ai/tool/dto/output/`. | One location, one naming convention. |
| Outputs MUST NOT include: `creadoPor`, `actualizadoEn`, persistence timestamps, raw SQL, stack traces, JWTs, internal handles, cross-owner data. | Prevents information leakage through the model surface. |
| Output fields MUST match the editable fields the use case actually persists. | Surfaces stage/loss-reason only when the use case changed them. |
| Plain bounded records with canonical component names are the default. Each component name is part of the model-visible JSON contract, so renaming a component is an explicit breaking tool-contract change that MUST be caught by a `ToolCallback.call(...)` JSON assertion. | Spring AI 2.0's default result converter uses Jackson 3 and serializes canonical record component names directly. |
| Use `tools.jackson.annotation.JsonProperty` only when an intentionally required JSON key differs from the Java component name. Never use Jackson 2 `com.fasterxml.jackson.annotation.JsonProperty` to control Spring AI 2.0 tool-result serialization. | Avoids redundant annotations and prevents relying on annotations from the wrong Jackson generation. |

## Write-tool authorization, idempotency, and audit

| Rule | Why |
|------|-----|
| Every write tool MUST require the authenticated actor via `ToolContext` before the use case runs. This validates trusted request context; it does not by itself prove target authorization or persisted audit. | Identity discipline without overstating downstream enforcement. |
| Every write tool MUST delegate to a use case that is the source of truth for authorization semantics — do not re-implement ownership/role checks in the tool. | Avoids drift between tool and REST surfaces. |
| A tool MAY fail closed on missing trusted identity required to construct a valid command, including the trusted super-user claim for `PREDETERMINADA`; this does not replace Application authorization or invariants. | Trust-boundary validation and business authorization have different owners. |
| **DEVELOPMENT-ONLY TECHNICAL DEBT:** `edit_trato` validates that trusted actor context exists, then delegates to `EditTratoUseCase`, which does NOT receive or check that actor. Therefore the current backend path performs no actor-aware target authorization for this mutation. This gap is accepted temporarily so the maintainer can observe how the LLM uses the tool. Production safety requires adding `actorUsuarioId` to `EditTratoCommand` (or a parallel authorization adapter) and enforcing it in the use case. Do NOT close this gap in tooling code; redesign the Application authorization model in a dedicated task. | Honest record of the gap; the rules remain normative for production. |
| Idempotency for write tools MUST live in the Application layer (e.g. via the agent tool-action ledger), not in the tool. | The tool should stay a thin mapper. |
| Write effects MUST be auditable through the existing Application logging or the durable agent-tool-action ledger; the tool MUST NOT add its own audit logging. | One audit story, owned by Application. |

## User control of write inputs

Schema-required means the backend needs a value, not that the agent may invent it.
The system prompt governs all write tools: important business choices must come
from the owner's current conversation or scoped permission to choose them.
Ask for missing choices or that permission, then wait before writing. Existing
permission (including in the same request) needs no repeated confirmation;
report agent-chosen values after completion. Never invent optional facts. Omit
optional inputs only when supported without unwanted changes; do not require a
questionnaire for every optional field. Only documented defaults may be used.

For boards, prefer owner-provided name, description, and type. A name-only request
must trigger a request for description and TAREAS/TRATOS, or permission to fill
them. The backend still requires all three; this does not make them nullable.
Use available reads to resolve references or preserve unchanged fields; ask when
ambiguous or unavailable. Never guess UUIDs or silently clear full replacement
sets (labels/order). Read-only queries do not require write confirmation.

For `edit_contact`, `edit_company`, `edit_tarea`, `edit_agenda`, and `edit_trato`,
omitted or null optional values preserve the existing value because their
canonical edit commands are full replacements. Empty strings set optional text
to blank, and Contacto/Empresa/Agenda normalize such strings to null; this does
not provide a clear operation for nullable UUID links or agenda end time.
Changing a nullable UUID requires an explicit replacement UUID. These reads and
edits are separate use-case calls, not an atomic patch; concurrent edits can
race. Company lookup uses the canonical all-companies use case because no
get-by-id use case exists; it is not target authorization. `edit_agenda` uses the existing reminder flag
and lead time when they are omitted; enabling reminders requires an existing or
explicitly provided positive `minutosAntes`. `edit_trato` cannot clear its
optional value/probability/close-date fields through null; `tipoContrato` is
required, matching REST validation. `create_trato` creates its initial Ficha
through the canonical use case; the tool must never create a second card.

This is prompt guidance, not a server-enforced approval gate. Contract tests
verify instructions/schema delivery, not live model compliance. Manual checks:
name-only board asks before creating; all three values supplied creates without
repeating questions; explicit permission fills missing values and reports them;
editing an unrelated card field does not silently clear its labels.

## Focused contract and wiring tests

For every new (or modified) tool, add or update the following tests:

| Test class | Location | Must cover |
|------------|----------|------------|
| `SpringAiCrmToolsTest` | `infrastructure/.../test/.../adapter/out/ai/tool/` | Allowlist exact 50 names; each tool's discovery carries real annotation metadata; the generated JSON schema requires exactly the documented fields; schemas never expose actor/owner/turn/handle; mapper validation is marked explicitly; model-visible errors use stable validation/execution codes and never expose downstream exception text. |
| `CrmToolMapperTest` | `infrastructure/.../test/.../adapter/out/ai/tool/` | Mapper accepts valid required + optional inputs; rejects null/blank required inputs with the documented message; rejects unknown enum names; maps domain entity to bounded output. |
| `AgentConfigTest` | `boot/.../test/.../config/` | The real two-response tool loop uses the configured redacting manager; the production default-system template advertises every allowlisted tool by name. |
| `AgentConfigTest` | `boot/.../test/.../config/` | The configured client registers all 50 tools by default and uses the redacting tool loop. |
| `AgentConfigOpenAiWiringTest` | `boot/.../test/.../config/` | Provider wiring resolves the production `ChatClient` and tool error processor. |
| `AgentConversationWiringTest` | `boot/.../test/.../config/` | The canonical use case backing each write tool is wired exactly once. |

When you remove a tool, remove its wiring bean, its mapper methods,
its output DTO, and every test entry that references it. Do NOT leave
half-removed artifacts.

## Template checklist for adding a future tool

Copy and complete this checklist when adding a new tool.

```markdown
### Tool: `<name>`

- [ ] Existing Application use case: `<fully.qualified.UseCase>` accepts `<command>`
- [ ] `<Resource>Tools` method: `<javaMethodName>` with `@Tool(name = "<snake_name>", description = "...")`
- [ ] `@ToolParam` set on every parameter; required fields documented in the description
- [ ] Output record created under `dto/output/`
- [ ] `CrmToolMapper.to<Name>Command(...)` validates required fields, parses enums, and rejects unknowns
- [ ] `CrmToolMapper.to<Name>Output(...)` projects domain entity to the bounded output record
- [ ] `@Tool` returns that concrete output record directly; no tool-local `ObjectMapper` serialization
- [ ] A `ToolCallback.call(...)` test asserts the exact model-visible JSON keys; any intentional key/component mismatch uses Jackson 3 `tools.jackson.annotation.JsonProperty`
- [ ] `WiringConfig` injects canonical use cases into the resource-specific tool bean
- [ ] `AgentConfig.DEFAULT_SYSTEM_TEMPLATE` advertises the new tool by name
- [ ] Tests added in `SpringAiCrmToolsTest`, `CrmToolMapperTest`, `AgentConfigTest`, `AgentConfigOpenAiWiringTest`, `AgentConversationWiringTest`
- [ ] Allowlist test still asserts exactly the right set of tool names
- [ ] No raw `ToolContext` field, actor id, or owner subject leaks into the generated schema
- [ ] No business logic, repository access, or authorization check inside the tool method
- [ ] Authorization gap (if any) recorded as **DEVELOPMENT-ONLY TECHNICAL DEBT** in this file
- [ ] OpenSpec `agent-crm-tools/spec.md` updated with the tool's contract and a scenario
```

## Tool inventory

The catalog is registered through nine controller-aligned classes. Each tool
delegates to the same canonical Application use case used by its controller.

| Group | Tool names |
|-------|------------|
| `ContactoTools` | `find_contacts`, `create_contact`, `get_contact`, `edit_contact`, `change_contact_state`, `delete_contact` |
| `EmpresaTools` | `create_company`, `list_companies`, `edit_company`, `change_company_state`, `delete_company` |
| `TratoTools` | `create_trato`, `list_tratos`, `get_trato`, `edit_trato`, `delete_trato` |
| `TareaTools` | `create_tarea`, `list_tareas`, `get_tarea`, `edit_tarea`, `delete_tarea` |
| `EtiquetaTools` | `create_etiqueta`, `list_etiquetas`, `get_etiqueta`, `edit_etiqueta`, `delete_etiqueta` |
| `AgendaTools` | `create_agenda`, `list_agendas`, `get_agenda`, `edit_agenda`, `delete_agenda` |
| `TableroTools` | `list_tableros`, `get_tablero`, `create_tablero`, `edit_tablero`, `delete_tablero`, `eliminar_columna_del_tablero`, `assign_columna_to_tablero`, `reorder_tablero_columns` |
| `ColumnaTools` | `list_columnas`, `get_columna`, `create_columna`, `edit_columna`, `delete_columna` |
| `FichaTools` | `list_fichas`, `get_ficha`, `create_ficha`, `edit_ficha`, `delete_ficha`, `move_ficha_to_columna` |

Delete tools are exposed, including `delete_company`. Destructive calls require a
clear user request; `delete_etiqueta` additionally requires an explicit
confirmation argument that defaults to `false`. The authenticated actor and,
where required, super-user identity come from trusted `ToolContext`, never model
arguments. This identity check does not guarantee target ownership: many
canonical CRUD use cases do not enforce actor/tenant scoping, and the tools do
not add or claim stronger authorization.

Agenda creator/list identity is resolved from trusted actor context. `create_tarea`
uses its canonical use case's existing initial-card behavior and must not create a
second Ficha. Ficha output labels may be truncated; never use that output to
reconstruct a full replacement set. All tool outputs remain bounded DTOs, not
raw entities.
