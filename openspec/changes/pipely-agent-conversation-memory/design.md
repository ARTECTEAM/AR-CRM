# Design: Pipely Agent Conversation Memory — Current CRM Tool Surface

## Technical Approach

Preserve `POST /api/agent/messages`, visible history, durable-memory recall, Spring AI completion, and one shared DefaultTools bean. Keep each tool as a thin adapter that validates model arguments and delegates to an existing canonical Application use case.

The active catalog contains exactly six tools: `find_contacts`, `create_contact`, `edit_contact`, `create_company`, `edit_company`, and `edit_trato`. WhatsApp integrations, deal notes, stage-specific writes, legacy list filters, and `find_companies` are outside the current surface.

## Architecture Decisions

| Decision | Current choice and rationale |
|---|---|
| Tool boundary | Trusted actor data comes from server-built `ToolContext`, remains outside model schemas, and fails closed when absent. |
| Application reuse | Tools call the same canonical use cases used by other adapters. No agent-specific business use-case layer remains. |
| Contact search | Preserve the actor-scoped database query and the 20-result cap. |
| Company access | Keep company create/edit. Company search is removed until a replacement query contract is designed. |
| Deal editing | `edit_trato` delegates to `EditTratoUseCase`. Actor-aware deal authorization remains documented development debt in the downstream use case. |
| Removed integrations | The WhatsApp/Evolution/n8n/Anthropic module and its controllers, persistence, security, wiring, configuration, and schema are removed. |
| Legacy deal states | Persisted `GANADO` and `PERDIDO` values are normalized to `CERRADO` before JPA enum hydration. |

## Data Flow

```text
JWT ActorContext → CompleteUserTurnService → ChatCompletionPort
  → server-built ToolContext → SpringAiCrmTools
  → CrmToolMapper validation → canonical Application use case
  → bounded tool output → assistant response
```

## Main Components

| Component | Responsibility |
|---|---|
| `SpringAiCrmTools` | Declares the six model-visible tools and validates trusted context presence. |
| `CrmToolMapper` | Normalizes model arguments and maps bounded outputs. |
| `AgentConfig` | Registers one shared DefaultTools bean and advertises the six-tool catalog. |
| `WiringConfig` | Injects only the canonical use cases required by the active tools. |
| Agent conversation/memory packages | Preserve turn idempotency, visible history, and durable recall independently of removed CRM integrations. |
| `schema.sql` | Retains non-WhatsApp compatibility patches and normalizes legacy deal states. |

## Removed Surface

- WhatsApp channels, conversations, messages, groups, media, webhooks, SSE, bots, CSAT, autoresponders, Evolution, n8n, and Anthropic suggestion.
- Deal notes/timeline and ganar/perder-specific domain behavior.
- Empresa, trato, tarea, ficha, and tablero filter-criteria verticals.
- `find_companies` and its bounded output.
- `AgentCrmWriteUseCase`, `AgentCrmWriteService`, and stage-specific tool contracts.

## Testing Strategy

| Layer | Proof |
|---|---|
| Domain/Application | Current `Trato` state model and canonical use-case contracts compile and pass focused tests. |
| Infrastructure | Six-tool schemas/mapping pass; actor-scoped contact search remains; legacy deal rows normalize before repository hydration. |
| Boot | Shared DefaultTools wiring and six-tool inventory pass focused context tests. |
| Package | All remaining Maven modules package successfully. |

Full `mvn verify` still reports the unrelated baseline `TableroControllerIT` authorization mismatch (`201` expected, `403` actual). It is not evidence against this removal change.

## Migration and Rollout

`schema.sql` idempotently maps existing `tratos.estado` values `GANADO` and `PERDIDO` to `CERRADO`. Existing WhatsApp tables are not dropped automatically; database cleanup is a separate operator decision.

The maintainer-approved delivery is one PR with an explicit `size:exception`, organized as two work-unit commits: legacy runtime/product removal plus data compatibility, followed by the six-tool contract, active documentation, and verification evidence. No push has occurred yet; this design does not create or claim a feature-branch chain.
