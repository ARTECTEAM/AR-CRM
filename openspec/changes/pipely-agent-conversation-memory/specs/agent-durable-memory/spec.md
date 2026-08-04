## agent-durable-memory

### Requirements
- **Durable Memory Is an MVP Capability:** The system MUST persist and recall eligible durable memory; a permanently empty/no-op implementation MUST NOT satisfy acceptance. It remains separate from visible history and chat-window truncation.
- **Explicit Safe Eligibility:** Only an explicit request to remember a preference, instruction, or stated memory MAY be retained. Conversation/tool outcomes MAY inform the lifecycle but MUST NOT automatically retain inferred content, secrets, credentials, authentication material, or arbitrary raw tool payloads.
- **Owner-Bound Lifecycle:** Reads/writes MUST be owner-isolated. Supersession requires a unique explicit active owner target; expiry and purge follow agreed retention. Deleted, superseded, expired, and purged memory MUST be unavailable. The v1 contract does not define or require the outcome of an ambiguous update/supersession request; that resolution is deferred to v2.
- **Deterministic Eligible Reconstruction:** Every eligible active owner memory MUST appear exactly once in stable deterministic order, separately from visible history.

### Scenarios
#### Scenario: Eligible memory is recalled independently of visible history
- GIVEN eligible active memory exists outside the visible-history window
- WHEN a later owner request reconstructs context
- THEN the eligible memory is recalled separately from visible history

#### Scenario: Sensitive content without eligibility is rejected
- GIVEN content contains sensitive material without an explicit remember request
- WHEN durable memory eligibility is evaluated
- THEN the content is not persisted

#### Scenario: Repeated reconstruction preserves deterministic order
- GIVEN multiple eligible active memories belong to one owner
- WHEN context reconstruction is repeated
- THEN each memory appears exactly once in the same stable order

### V1 Boundary and V2 Deferred Non-Goal
Owner isolation, explicit eligibility, eligible recall, deterministic ordering, and the defined durable-memory lifecycle remain v1 acceptance behavior. Resolving an ambiguous durable-memory update or supersession request is explicitly deferred to v2 and MUST NOT be treated as a v1 acceptance criterion.

### Superseded MVP Statement
The former post-MVP/empty-list boundary is superseded; persistence, eligibility, ordering, recall, expiry, and purge are MVP acceptance behavior.
