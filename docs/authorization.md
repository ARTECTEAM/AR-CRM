# Configurable CRM roles

CRM permissions are configured per role in the existing shared CRM environment. A role starts from a small editable preset or from no access; there is no organization, workspace, or tenant boundary.

## Quick setup

1. Set `CRM_AUTHORIZATION_BOOTSTRAP_ADMIN_SUBJECT` to the exact validated Keycloak JWT `sub` of an existing, active local CRM user whose current role is also active.
2. Restart CRM2 and sign in as that user. Create an editable preset snapshot with `POST /api/roles/create`, for example `{"nombre":"CRM administrator","plantilla":"ADMINISTRADOR"}`.
3. Assign that role to another user with `POST /api/usuarios/create` or `PUT /api/usuarios/edit?id=...`. A user cannot change their own role assignment or their current role's permissions. If this is the first administrator and the bootstrap subject is the only local user, it may assign itself an active role with shared-scope `ROL.ADMINISTRAR` once, but only while no active role manager exists; the database lock makes concurrent first promotions serialize. Otherwise, provision a second user and assign the role to that account.
4. Configure grants via `PUT /api/roles/edit?id=...`; an explicit `permisos` array replaces the grants, `[]` revokes them, and an omitted/null value preserves them.

The bootstrap subject is not a username, email, or `usuario_id` claim. It can manage roles and user assignments but does not automatically receive access to CRM records, financial values, or private contact fields. The one-time self-promotion is limited to an active shared-scope role manager and is unavailable after any active role manager exists. `SUPER_USUARIO` remains a separate technical Keycloak authority and does not bypass CRM grants. All non-preflight requests to `/api/superusuarios/**` require this authority; `OPTIONS` preflight remains public. If the property is unset or does not match an existing active local user with an active role, role/user management fails closed.

Privileged role and user mutations acquire the ordered role locks in a database transaction before reloading the actor and target state, checking authority, and applying the local change. User creation may provision the external identity first, but it repeats the role and delegation checks under lock before saving and compensates by deleting the provisioned identity if that final check fails. User edits hold the role lock across Keycloak email/enabled synchronization and local persistence; this favors authorization consistency over throughput and can serialize role/user writes during a slow identity-provider call. User deletion commits the local removal under lock before best-effort Keycloak cleanup.

## Permission model

| Setting | Behavior |
|---|---|
| Actions | `LEER`, `CREAR`, `ACTUALIZAR`, `ELIMINAR`, `ADMINISTRAR`; `ADMINISTRAR` means full control of that resource. |
| Row scope | Shared records, own/assigned records, or explicitly allowed boards. Unsupported scope/resource combinations deny access. |
| Field groups | `FINANCIERO` and `CONTACTO_PRIVADO` each have separate read/write grants. Writing a group requires read permission for that group. |
| Presets | Presets create editable grant rows; they are not special runtime identities. |
| Existing roles | Roles created before this feature have no grants and stay denied until an administrator explicitly edits them. |

Field masking is applied to REST and agent-tool projections. It removes values rather than replacing hidden amounts with zero. Contact email/phone, company phone/internal notes, deal value/probability/close date, and board-column deal totals use the relevant field group. `SuperUsuarioResponse` omits the internal Keycloak ID; passwords and credentials are never returned.

## Limits and migration

This release enforces actions, supported row scopes, and grouped field visibility. It does not claim daily usage quotas or rate limits. Role grants are persisted in `rol_permisos`; existing roles are deliberately not backfilled with broad access. Agent authorization revisions are checked before tool dispatch, after model completion, and before cached history or memory is returned. A revocation between the final check and persistence or HTTP delivery may not cancel a response already finalizing; the interaction does not provide atomic cancellation or linearizable delivery. Future model context excludes history and durable memories whose stored revision no longer matches. Non-null `usuarios.keycloak_id` values are unique; if legacy data has duplicates, remove or reconcile them explicitly before deployment, because startup will fail while creating the unique index rather than guessing which local identity should own a Keycloak subject. Multiple null links remain allowed.
