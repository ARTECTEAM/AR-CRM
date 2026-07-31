## agent-durable-memory

### Requirements
- **Durable Memory Is an MVP Capability:** The system MUST persist and recall eligible durable memory; a permanently empty/no-op implementation MUST NOT satisfy acceptance. It remains separate from visible history and chat-window truncation.
- **Explicit Safe Eligibility:** Only an explicit request to remember a preference, instruction, or stated memory MAY be retained. Conversation/tool outcomes MAY inform the lifecycle but MUST NOT automatically retain inferred content, secrets, credentials, authentication material, or arbitrary raw tool payloads.
- **Owner-Bound Lifecycle:** Reads/writes MUST be owner-isolated. Supersession requires a unique explicit active owner target; expiry and purge follow agreed retention. Deleted, superseded, expired, and purged memory MUST be unavailable.
- **Deterministic Eligible Reconstruction:** Every eligible active owner memory MUST appear exactly once in stable deterministic order, separately from visible history.

### Scenarios
- Eligible memory is recalled independently of the visible-history window.
- Sensitive content without an eligible request is not persisted.
- An ambiguous memory update supersedes nothing; repeated reconstruction preserves order.

### Superseded MVP Statement
The former post-MVP/empty-list boundary is superseded; persistence, eligibility, ordering, recall, expiry, and purge are MVP acceptance behavior.
