## security (delta)

### Modified Requirement: Path-Accurate Endpoint Security Coverage
The system MUST verify actual protected paths. A valid JWT MUST establish immutable ActorContext before model, memory, or tool work. Only trusted context MAY determine actor, owner, tenant, permissions, or idempotency identity; request/prompt/model/tool arguments MUST NOT override it. Conversation, memory, and tool results MUST be owner-isolated, and existing CRM permission/ownership checks MUST apply to each tool. Internal handles and sensitive prompt/tool/response/memory content MUST remain out of public responses and default observability.

### Scenarios
- Valid protected paths authorize and missing credentials return 401 before model, memory, persistence, or tools.
- An owner override attempt cannot access another owner's conversation, memory, data, or tool results.
- An actor lacking current CRM permission/ownership cannot disclose or mutate the target.

### Superseded Statement
The former authentication-only MVP boundary is superseded: existing CRM permission and ownership enforcement is mandatory for tool execution.
