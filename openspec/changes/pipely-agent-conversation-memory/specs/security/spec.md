## security (delta)

### Modified Requirement: Path-Accurate Endpoint Security Coverage
The system MUST verify actual protected paths as v1 behavior. A valid JWT MUST establish immutable ActorContext before model, memory, or tool work. Only trusted context MAY determine actor, owner, tenant, permissions, or idempotency identity; request/prompt/model/tool arguments MUST NOT override it. The trusted actor and owner context MUST be available to the authorization decision for every allowlisted CRM tool, including `update_deal_stage`. Conversation, memory, and tool results MUST be owner-isolated, and existing CRM permission/ownership checks MUST remain mandatory v1 acceptance criteria rather than a v2 deferral. Internal handles and sensitive prompt/tool/response/memory content MUST remain out of public responses and default observability.

### Scenarios
#### Scenario: Missing credentials are rejected before protected work
- GIVEN a request without valid JWT credentials
- WHEN it reaches the conversational ingress
- THEN the system returns 401 before model, memory, persistence, or tool work

#### Scenario: Owner override cannot cross isolation boundaries
- GIVEN an authenticated actor attempts to override another owner's identity
- WHEN the request accesses conversation, memory, data, or tool results
- THEN the system denies the cross-owner access

#### Scenario: CRM authorization remains enforced for every tool
- GIVEN an actor lacks current CRM permission or ownership for a target
- WHEN any allowlisted CRM tool, including `update_deal_stage`, is invoked
- THEN the system denies disclosure or mutation using trusted actor context

### Superseded Statement
The former authentication-only MVP boundary is superseded: existing CRM permission and ownership enforcement is mandatory for tool execution.
