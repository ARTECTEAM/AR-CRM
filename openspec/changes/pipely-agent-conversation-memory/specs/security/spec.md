## security (delta)

### Modified requirement: trusted identity and production registration

A valid JWT MUST establish immutable `ActorContext` before model, memory, or tool work. `usuarioId` and optional `superUsuarioId` are distinct claims. Only server-created context MAY carry them into tools; prompt/model arguments MUST NOT expose or override them.

The production/default catalog MUST contain exactly six callbacks. The 15 actor/tenant-unscoped tablero/columna/ficha callbacks MUST require both `crm2.agent.development-tools-enabled=true` and exactly one active profile equal to `noauth` or `test`. Flag true with no profile, `noauth,test`, or any production/unrecognized/mixed profile MUST fail startup with a stable configuration error. Prompt text alone is not an enforcement boundary.

All tool errors returned to the model MUST be redacted unless the originating validation exception is explicitly allowlisted as safe. Prompts, arguments, results, credentials, SQL, and sensitive exception details MUST remain out of logs and default observability.

### Current authorization debt

- `edit_trato` requires trusted actor presence but its use case is actor-free.
- tablero list/get/create/edit/assign/reorder does not enforce actor/tenant ownership.
- columna list/get/create/edit does not enforce actor/tenant ownership; only the real optional super-user claim is propagated for creation.
- ficha list/get/create/edit/move does not enforce actor/tenant ownership.
- The action ledger and CRM mutation are not one atomic transaction, so write retry convergence is not claimed.

These are production blockers, not prompt policies. Development opt-in does not waive privilege-escalation, credential-leakage, irreversible-loss, or mutation-semantics protections.

Security behavior is delivered with its owning slice in the seven-slice Feature Branch Chain in `design.md`, especially trusted identity transport (slice 1), safe default-tool execution (slice 2), unregistered callback/mutation slices (4–6), and environment-safe registration (slice 7). Documentation and verification are not detached; shared files require hunk-level staging/reconstruction, and each target-<=400-line intermediate branch must compile and pass focused tests.

### Scenarios

#### Missing actor
- GIVEN any registered callback without trusted actor context
- WHEN it is invoked
- THEN it fails before Application delegation

#### Identity separation
- GIVEN a normal user has `usuarioId` but no `superUsuarioId`
- WHEN a tool context is built
- THEN no super-user entry exists and the normal user is never promoted

#### Default production posture
- GIVEN the flag is absent/default or explicitly false in any environment
- WHEN callback registration and prompt catalog are inspected
- THEN only the six default tools are present and no tablero/columna/ficha callback is advertised

#### Production override fails closed
- GIVEN the flag is true under a production, unrecognized, mixed, or profile-less runtime
- WHEN configuration is created
- THEN startup fails loudly with the stable development-tool configuration error

#### Downstream failure
- GIVEN a downstream exception containing internal detail
- WHEN Spring AI processes the tool failure
- THEN the model receives a stable redacted code/message without that detail
