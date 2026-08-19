## agent-crm-tools

### Requirements
- **Fixed Allowlist and Trusted Delegation:** The system MUST execute exactly `find_contacts`, `create_contact`, `edit_contact`, `create_company`, `edit_company`, and `edit_trato`; aliases/unknown calls MUST NOT execute. Tools delegate through Application use cases, never repositories. Model actor, owner, tenant, permission, and idempotency values are ignored/rejected in favor of trusted context.
- **Find Contacts:** `find_contacts` MUST be read-only, return contacts visible under current CRM policy, and MAY use existing text, relationship-state, company, responsible-user, and source filters. It returns structured business data only.
- **Create Contact:** `create_contact` MUST use the existing creation contract: company identity, name, and relationship state are required; optional fields are validated; creator/owner/audit identity comes from trusted context. It returns canonical contact data and safely converges on retries.
- **Edit Deal:** `edit_trato` MUST delegate to the existing `EditTratoUseCase` and MUST only expose the editable business fields the use case accepts (id, responsableId, nombre, valorEstimado, probabilidad, fechaCierreEsperada, tipoContrato). Non-editable deal state is preserved by the canonical use case and MUST NOT be advertised as editable input. The `responsableId` field is the deal's business responsible user, NOT the authenticated actor. The tool requires trusted actor context before delegation, but the current actor-free `EditTratoUseCase` neither receives that actor nor performs actor-aware authorization. This development-only gap is documented, not fixed by this change.
- **Controlled Tool Outcomes:** Outputs MUST be structured and MUST NOT expose SQL, credentials, stack traces, JWTs, internal handles, or cross-owner data. Reads MAY repeat; writes MUST be safely idempotent. Failures are controlled application/model outcomes.

### V1 Correctness Boundary
Actor-aware authorization remains a production acceptance criterion, but the current `edit_trato` path does not satisfy it because `EditTratoUseCase` receives no authenticated actor. Closing that gap requires a dedicated Application authorization change and is outside this removal pass; trusted-context validation at the tool boundary MUST NOT be represented as target authorization. Safe idempotency remains a CRM-write acceptance criterion: a retry with the same trusted action identity MUST NOT produce a second CRM effect.

### Exact Tool Scenarios
#### Scenario: Unknown tools are rejected
- GIVEN a model requests an unallowlisted or aliased tool
- WHEN the tool request is evaluated
- THEN it is not executed

#### Scenario: Contact search respects current visibility
- GIVEN an authenticated actor and matching contacts
- WHEN `find_contacts` is invoked
- THEN it returns only contacts visible under the actor's current CRM policy

#### Scenario: Contact creation uses trusted identity
- GIVEN valid required contact data and a trusted actor
- WHEN `create_contact` is invoked
- THEN it returns one canonical contact without repeating the effect on the same action retry

#### Scenario: Deal-edit validation and the authorization gap are explicit
- GIVEN an existing deal, trusted actor context, and either a valid or unsupported input
- WHEN `edit_trato` is invoked
- THEN a valid input mutates the editable fields while the deal's state remains unchanged; an unsupported field or invalid value mutates nothing; and the contract does not claim actor-aware target authorization that the current use case does not perform

#### Scenario: edit_trato does not advertise non-editable deal state
- GIVEN the canonical edit use case preserves non-editable deal state
- WHEN the tool's JSON schema is introspected
- THEN it MUST expose only the documented editable fields

#### Scenario: CRM write retries are safely idempotent
- GIVEN a completed CRM write with a trusted action identity
- WHEN the same write action is retried
- THEN the canonical result is returned or converged without another CRM effect
