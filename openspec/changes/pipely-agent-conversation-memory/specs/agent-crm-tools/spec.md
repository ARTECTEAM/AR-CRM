## agent-crm-tools

### Requirements
- **Fixed Allowlist and Trusted Delegation:** The system MUST execute exactly `find_contacts`, `create_contact`, and `update_deal_stage`; aliases/unknown calls MUST NOT execute. Tools delegate through Application use cases, never repositories. Model actor, owner, tenant, permission, and idempotency values are ignored/rejected in favor of trusted context.
- **Find Contacts:** `find_contacts` MUST be read-only, return contacts visible under current CRM policy, and MAY use existing text, relationship-state, company, responsible-user, and source filters. It returns structured business data only.
- **Create Contact:** `create_contact` MUST use the existing creation contract: company identity, name, and relationship state are required; optional fields are validated; creator/owner/audit identity comes from trusted context. It returns canonical contact data and safely converges on retries.
- **Update Deal Stage:** `update_deal_stage` MUST use only the narrow deal-status operation. It requires deal identity and an allowed status: current contract supports `GANADO` and `PERDIDO` (with loss reason). The trusted actor and owner context MUST reach its authorization decision; model-supplied identity MUST NOT influence it. Permission/ownership checks apply; retried action identity produces no repeated CRM effect beyond the final status.
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

#### Scenario: Deal-stage authorization and validation are enforced
- GIVEN a visible deal, trusted actor context, and either an allowed or unsupported status
- WHEN `update_deal_stage` is invoked
- THEN an authorized allowed status changes the deal, while an unsupported status or unauthorized actor mutates nothing

#### Scenario: CRM write retries are safely idempotent
- GIVEN a completed CRM write with a trusted action identity
- WHEN the same write action is retried
- THEN the canonical result is returned or converged without another CRM effect
