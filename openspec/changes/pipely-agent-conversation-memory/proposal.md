# Proposal: Pipely Memory-Capable CRM Action Agent

> **Layers:** application | infrastructure | boot

## Intent

Sales users need one conversation that remembers relevant context and performs a few useful CRM actions without navigating multiple workflows. The MVP proves a **memory-capable action agent**, not a general autonomous agent.

## Scope

### In Scope
- One authenticated conversational endpoint: receive a request, derive the actor, reconstruct memory, invoke the LLM/tools, persist conversation state, and return the final assistant response.
- An explicit Spring AI tool allowlist of six shared CRM tools: `find_contacts`, `create_contact`, `edit_contact`, `create_company`, `edit_company`, and `edit_trato`.
- Tools delegate only to existing Application use cases. Validated server-side JWT/`ActorContext` supplies actor/owner identity; model arguments cannot. Existing authentication, permission checks, owner/tenant isolation, and write idempotency remain mandatory where the backing Application contract supports them. `edit_trato` currently validates trusted actor presence but delegates to an actor-free use case, so actor-aware target authorization remains explicit development-only debt outside this removal pass.
- Persistent visible history with explicit `USER`/`ASSISTANT` roles; bounded model context reconstruction; durable owner memory with the agreed explicit lifecycle, eligibility, stable ordering, isolation, and cross-request recall. These remain separate context sources and never leak across owners.

### Out of Scope
- Any CRM tool/effect beyond the six allowlisted tools, arbitrary discovery, or repository exposure.
- Public completion/regeneration routes, public opaque handles, or inferred regeneration behavior.
- RAG, vector stores, MCP, and streaming/multimodal/structured-output platform work unless a selected tool contract strictly requires it.
- Advanced concurrent convergence, handle rotation/expiry, exhaustive provider recovery, exhaustive confirmation/compensation, and exhaustive edge-case handling.

## Capabilities

### New Capabilities
- `agent-conversation`: one owner-isolated interaction, visible history, bounded context, and final response.
- `agent-durable-memory`: durable explicit owner memory, lifecycle, deterministic eligibility/order, and recall.
- `agent-crm-tools`: controlled execution of the six allowlisted CRM actions through Application use cases.

### Modified Capabilities
- `security`: JWT-derived actor propagation, owner/tenant isolation, preservation of existing permission checks where supported, and explicit tracking of the actor-free `edit_trato` authorization gap.

## Approach

Application orchestration composes the completed PR1–PR5 turn/history foundation with durable memory, Spring AI tools, and one REST interaction. Deliver durable memory before or with orchestration; tools/orchestrator, endpoint, and minimal wiring follow. PR1–PR5 history remains unchanged.

## Affected Areas

| Area | Impact | Description |
|---|---|---|
| `application/.../agent/` | Modified | Conversation, memory, and tool orchestration |
| `infrastructure/.../{ai,persistence,rest}/` | Modified | Spring AI, owner memory, and single ingress |
| `boot/.../WiringConfig.java` | Modified | Minimal composition |

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Cross-owner data/effects | Medium | Trusted actor context and owner-scoped use cases |
| Duplicate writes | Medium | Preserve idempotency boundaries |
| MVP expands into general automation | High | Fixed allowlist and explicit non-goals |

## Rollback Plan

Disable the conversational ingress and tool registration; retain owner-isolated records under existing retention rules.

## Dependencies

Validated JWT/`ActorContext`, existing CRM use cases and permissions, Spring AI, and owner-scoped persistence.

## Success Criteria

- [ ] A valid owner request can use each allowlisted tool and return a final response; unauthenticated work is rejected, and cross-owner work is rejected by actor-aware Application contracts. The current `edit_trato` exception remains blocked from production acceptance until actor-aware authorization is added.
- [ ] Later requests reconstruct role-preserving bounded history and eligible durable memory without cross-owner leakage.
- [ ] Retries do not duplicate visible conversation state or CRM writes.
- [ ] No non-allowlisted tool or public completion/regeneration surface exists.
