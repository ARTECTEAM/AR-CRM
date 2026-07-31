## agent-crm-tools

### Requirements
- **Fixed Allowlist and Trusted Delegation:** The system MUST execute exactly `find_contacts`, `create_contact`, and `update_deal_stage`; aliases/unknown calls MUST NOT execute. Tools delegate through Application use cases, never repositories. Model actor, owner, tenant, permission, and idempotency values are ignored/rejected in favor of trusted context.
- **Find Contacts:** `find_contacts` MUST be read-only, return contacts visible under current CRM policy, and MAY use existing text, relationship-state, company, responsible-user, and source filters. It returns structured business data only.
- **Create Contact:** `create_contact` MUST use the existing creation contract: company identity, name, and relationship state are required; optional fields are validated; creator/owner/audit identity comes from trusted context. It returns canonical contact data and safely converges on retries.
- **Update Deal Stage:** `update_deal_stage` MUST use only the narrow deal-status operation. It requires deal identity and an allowed status: current contract supports `GANADO` and `PERDIDO` (with loss reason). Permission/ownership checks apply; retried action identity produces no repeated effect beyond final status.
- **Controlled Tool Outcomes:** Outputs MUST be structured and MUST NOT expose SQL, credentials, stack traces, JWTs, internal handles, or cross-owner data. Reads MAY repeat; writes MUST be safely idempotent. Failures are controlled application/model outcomes.

### Exact Tool Scenarios
- An unallowlisted or aliased tool request is not executed.
- `find_contacts` returns only matching contacts visible to the actor.
- `create_contact` returns one canonical contact for valid required data and trusted actor.
- `update_deal_stage` marks a visible deal `GANADO`; an unsupported status mutates nothing.
- A retried write action identity returns/converges to the canonical result without another effect.
