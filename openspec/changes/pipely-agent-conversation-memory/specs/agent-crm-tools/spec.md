## agent-crm-tools

### Requirements

- **Default catalog:** With `crm2.agent.development-tools-enabled` absent or false, the system MUST register and advertise exactly `find_contacts`, `create_contact`, `edit_contact`, `create_company`, `edit_company`, and `edit_trato`.
- **Development catalog:** With the explicit flag true and exactly one active profile equal to `noauth` or `test`, the system MUST additionally register exactly the 15 tablero/columna/ficha callbacks listed in the proposal, for a total of 21. Flag true with no profile, `noauth,test`, or any production/unrecognized/mixed profile MUST fail startup with the stable configuration error. No delete/remove callback MAY be registered in either successful mode.
- **Trusted context:** Actor, owner, tenant, turn, and optional super-user identity MUST come only from server-created context and MUST be absent from model-visible schemas. `usuarioId` MUST NOT be converted into `superUsuarioId`.
- **Column creation:** `create_columna` MUST pass the actual optional trusted `superUsuarioId`. `PREDETERMINADA` MUST fail before delegation when the claim is absent. `PERSONALIZADA` MAY delegate with no super-user identity.
- **Card edit labels:** `edit_ficha.etiquetaIds` MUST be required and represent the complete replacement set; explicit `[]` means clear all. Omission MUST NOT be interpreted as clear-all.
- **Bounded results:** All top-level and nested collections MUST be deterministically ordered, hard-capped, null-normalized, and defensively copied. Sentinel-backed `find_contacts` MUST report `returned` and truthful `truncated` without claiming an exact total. Fully materialized development lists MUST report exact `total` and truthful `truncated`; fully materialized nested collections MUST report their corresponding exact totals/truncation flags.
- **Safe failures:** Arbitrary runtime/downstream messages MUST NOT be model visible. Only explicitly marked safe validation messages MAY be returned; all other failures MUST use a stable redacted code/message.
- **Application delegation:** Tools MUST call Application use cases, never repositories. Trusted-context presence MUST NOT be described as actor/tenant authorization where the backing use case is unscoped.

### Development-only blockers

The 15 tablero/columna/ficha tools are not production-safe: current use cases are actor/tenant unscoped. `edit_trato` remains actor-free downstream. The existing durable action ledger does not atomically include CRM writes, leaving a crash window between mutation and action completion. Therefore this spec does not claim retry convergence for these writes; production enablement requires a new atomic design and repeat-invocation effect tests.

Delivery follows the seven-slice Feature Branch Chain in `design.md`: trusted identity transport; safe default-tool execution loop; bounded output contracts; unregistered read-only development callbacks; unregistered tablero/columna mutations; unregistered ficha mutations; then environment-safe registration/catalog integration. Each slice carries its own tests, documentation, and verification, targets at most 400 changed lines where technically feasible, uses hunk-level staging/reconstruction for shared files, and must compile and pass focused tests before the next slice.

### Scenarios

#### Default startup
- GIVEN the development flag is absent or false
- WHEN the CRM `ChatClient` is built
- THEN exactly six callback schemas and exactly those six prompt catalog names are present

#### Development startup
- GIVEN the development flag is true and the only active profile is `noauth` or `test`
- WHEN the CRM `ChatClient` is built
- THEN exactly 21 non-delete callback schemas and names are present

#### Production startup rejects development tools
- GIVEN the development flag is true and no profile or a production/unrecognized/mixed profile is active
- WHEN configuration is created
- THEN startup fails with the stable development-tool configuration error

#### Trusted super-user propagation
- GIVEN distinct trusted `usuarioId` and `superUsuarioId` claims
- WHEN `create_columna` is invoked
- THEN the command contains only the trusted `superUsuarioId`, and neither identity appears in the schema

#### Normal-user column creation
- GIVEN a trusted normal user with no super-user claim
- WHEN `PERSONALIZADA` is requested
- THEN creation delegates with no super-user identity
- WHEN `PREDETERMINADA` is requested
- THEN it fails before mutation

#### Label replacement
- GIVEN `edit_ficha`
- WHEN `etiquetaIds` is omitted
- THEN validation fails before mutation
- WHEN `etiquetaIds` is `[]`
- THEN the complete label set is cleared
