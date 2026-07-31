## agent-conversation

### Requirements
- **Single Conversational Interaction:** The system MUST accept one authenticated request with a message and idempotency identity, reconstruct owner-scoped context, invoke the model and approved tools as needed, persist final visible state, and return final assistant content.
- **Role-Bearing Visible History:** The system MUST persist chronological `USER`/`ASSISTANT` speaker provenance (not authorization roles), retain owner-scoped history for 12 months, and reconstruct a bounded completed oldest-to-newest window excluding the active turn.
- **Idempotent Final Interaction:** Same owner, identity, and normalized request MUST converge to one user message, one assistant message, and one final response; changed content MUST be rejected and incomplete attempts remain retryable.
- **Internal-Only Stages:** Completion, regeneration, and tool execution SHALL be internal. No public completion, regeneration, tool route, inferred regeneration, or internal handle exposure is allowed.

### Scenarios
- Valid request returns final content after owner-scoped context preparation.
- Same normalized request retry produces no duplicate visible messages or response.
- Direct completion, regeneration, or tool access has no public operation.

### Superseded MVP Statement
The previous prohibition on CRM effects is superseded: only the `agent-crm-tools` allowlist MAY produce CRM effects.
