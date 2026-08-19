## agent-crm-tools

### Requirements
- **Fixed Allowlist and Trusted Delegation:** The system MUST execute exactly `find_contacts`, `create_contact`, and `edit_trato`; aliases/unknown calls MUST NOT execute. Tools delegate through Application use cases, never repositories. Model actor, owner, tenant, permission, and idempotency values are ignored/rejected in favor of trusted context.
- **Find Contacts:** `find_contacts` MUST be read-only, return contacts visible under current CRM policy, and MAY use existing text, relationship-state, company, responsible-user, and source filters. It returns structured business data only.
- **Create Contact:** `create_contact` MUST use the existing creation contract: company identity, name, and relationship state are required; optional fields are validated; creator/owner/audit identity comes from trusted context. It returns canonical contact data and safely converges on retries.
- **Edit Deal:** `edit_trato` MUST delegate to the existing `EditTratoUseCase` and MUST only expose the editable business fields the use case accepts (id, responsableId, nombre, valorEstimado, probabilidad, fechaCierreEsperada, tipoContrato). The deal's stage (`estado`) and loss reason (`motivoPerdida`) are preserved by the canonical use case and MUST NOT be advertised as editable inputs. The `responsableId` field is the deal's business responsible user, NOT the authenticated actor. Trusted actor context reaches the tool for audit only; the canonical edit use case is the source of truth for authorization semantics and field set.
- **Controlled Tool Outcomes:** Outputs MUST be structured and MUST NOT expose SQL, credentials, stack traces, JWTs, internal handles, or cross-owner data. Reads MAY repeat; writes MUST be safely idempotent. Failures are controlled application/model outcomes.

### V1 Correctness Boundary
Authorization and ownership enforcement for every allowlisted tool, plus safe idempotency for CRM writes, are mandatory v1 acceptance criteria. A retry with the same trusted action identity MUST NOT produce a second CRM effect. Neither behavior is deferred to v2.

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

#### Scenario: Deal-edit authorization and validation are enforced
- GIVEN a visible deal, trusted actor context, and either a valid or unsupported input
- WHEN `edit_trato` is invoked
- THEN a valid input mutates the editable fields while the deal's stage and loss reason remain unchanged; an unsupported field or invalid value mutates nothing

#### Scenario: edit_trato does not advertise stage or loss reason
- GIVEN the canonical edit use case preserves the deal's stage and loss reason
- WHEN the tool's JSON schema is introspected
- THEN it MUST NOT expose `status` or `motivo` as editable fields

#### Scenario: CRM write retries are safely idempotent
- GIVEN a completed CRM write with a trusted action identity
- WHEN the same write action is retried
- THEN the canonical result is returned or converged without another CRM effect
