## security (delta)

### Modified Requirement: Path-Accurate Endpoint Security Coverage
The system MUST verify actual protected paths as v1 behavior. A valid JWT MUST establish immutable ActorContext before model, memory, or tool work. Only trusted context MAY determine actor, owner, tenant, permissions, or idempotency identity; request/prompt/model/tool arguments MUST NOT override it. Conversation, memory, and tool results MUST remain owner-isolated where the backing Application contract supports owner scope. Every allowlisted tool, including `edit_trato`, requires trusted actor context at its boundary. The current `edit_trato` path does not forward that actor to its actor-free use case and therefore does not implement actor-aware target authorization; this is explicit development-only debt outside the removal pass, not a completed security guarantee. Internal handles and sensitive prompt/tool/response/memory content MUST remain out of public responses and default observability.

### Scenarios
#### Scenario: Missing credentials are rejected before protected work
- GIVEN a request without valid JWT credentials
- WHEN it reaches the conversational ingress
- THEN the system returns 401 before model, memory, persistence, or tool work

#### Scenario: Owner override cannot cross isolation boundaries
- GIVEN an authenticated actor attempts to override another owner's identity
- WHEN the request accesses conversation, memory, data, or tool results
- THEN the system denies the cross-owner access

#### Scenario: CRM authorization uses trusted context where supported
- GIVEN an actor lacks current CRM permission or ownership for a target
- WHEN an allowlisted CRM tool backed by an actor-aware Application contract is invoked
- THEN the system denies disclosure or mutation using trusted actor context

#### Scenario: edit_trato authorization debt is not hidden
- GIVEN `edit_trato` receives trusted actor context
- WHEN it delegates to the current actor-free `EditTratoUseCase`
- THEN the specification does not claim actor-aware target authorization, and production enforcement remains a dedicated Application-layer follow-up

### Superseded Statement
The former authentication-only MVP boundary remains superseded for actor-aware CRM contracts. `edit_trato` is a documented temporary exception whose missing Application-layer authorization must not be represented as implemented.
