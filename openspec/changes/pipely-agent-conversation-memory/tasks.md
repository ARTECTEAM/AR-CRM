# Tasks: Pipely Agent Conversation Memory — Legacy Surface Removal

## Review Workload Forecast

| Field | Value |
|---|---|
| Current diff | More than 9,000 deletions across approximately 295 files |
| Review risk | High |
| Delivery strategy | One PR with explicit `size:exception` |
| Commit plan | Two work-unit commits |
| Push status | No push yet |

The maintainer approved one deletion-heavy PR under `size:exception`, not a feature-branch chain. Before any future push, organize the existing change as two work-unit commits: (1) legacy runtime/product surface removal plus data compatibility, and (2) the six-tool contract, active documentation, and verification evidence. No push has been performed or is authorized by this plan.

## Work Units

### 1. Remove WhatsApp and external integrations

- [x] Delete the `whatsapp` Maven module.
- [x] Remove Evolution, n8n bot, Anthropic suggestion, media, SSE, webhook, CSAT, autoresponder, and contact-sync adapters.
- [x] Remove WhatsApp controllers, DTOs, persistence, security filters, Boot wiring, configuration, and schema definitions.
- [x] Preserve non-WhatsApp Docker, Keycloak, CORS, SQL initialization, and deployment configuration.

### 2. Remove legacy CRM product surfaces

- [x] Remove deal notes/timeline domain, application, REST, persistence, and wiring.
- [x] Remove ganar/perder methods, loss-reason storage, and obsolete enum values.
- [x] Remove Empresa, Trato, Tarea, Ficha, and Tablero filter-criteria verticals.
- [x] Restore simple no-argument list contracts.
- [x] Preserve the actor-scoped contact search contract.

### 3. Reduce the Spring AI catalog

- [x] Remove `find_companies` and its mapper/output contract.
- [x] Keep `create_company` and `edit_company`.
- [x] Verify exactly six tools: `find_contacts`, `create_contact`, `edit_contact`, `create_company`, `edit_company`, and `edit_trato`.
- [x] Update Boot wiring, system prompt, tests, tool rules, and active OpenSpec contracts.

### 4. Migrate legacy deal state data

- [x] Normalize persisted `GANADO` and `PERDIDO` values to `CERRADO` through idempotent SQL.
- [x] Add repository-level regression coverage for repeated migration and enum hydration.
- [x] Preserve existing valid deal states.

### 5. Verification

- [x] Domain tests: 6 passed.
- [x] Trato migration/persistence tests: 3 passed.
- [x] Infrastructure Spring AI tests: 69 passed.
- [x] Boot wiring/configuration tests: 30 passed.
- [x] Multi-module package completed successfully.
- [x] Removed-symbol searches report no executable WhatsApp/Evolution surface or active filter-criteria, `find_companies`, deal-note, or ganar/perder contract; intentional safety, removal-context, migration, test, and report references remain.
- [x] `git diff --check` passed.

Full `mvn verify` remains non-green because the pre-existing `TableroControllerIT.create_shouldReturn201WithTableroJson` test expects `201` and receives `403`. This baseline authorization mismatch is tracked separately and is not part of the removal change.
