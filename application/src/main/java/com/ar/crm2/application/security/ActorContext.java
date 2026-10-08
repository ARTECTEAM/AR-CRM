package com.ar.crm2.application.security;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable record representing the authenticated actor for a request.
 *
 * This is a pure application-layer abstraction — no Spring Security, JWT,
 * or Keycloak types leak into domain/application layers.
 *
 * Token attributes are derived from a validated JWT, while {@code usuarioId} is
 * resolved from the active local CRM user linked to the token subject. The context
 * is assembled in infrastructure and passed to application services through
 * mapper code (never injected as a framework artifact).
 *
 * @param subject      Keycloak subject (sub claim) — the unique identity identifier
 * @param username     Keycloak preferred_username claim
 * @param email        Keycloak email claim (may be absent for service accounts)
 * @param usuarioId     Active local CRM Usuario ID resolved from the validated subject
 * @param superUsuarioId Custom Keycloak claim (super_usuario_id) — the CRM SuperUsuario entity ID
 * @param roles         Keycloak realm_access.roles — set of role strings assigned in the realm
 */
public record ActorContext(
    String subject,
    String username,
    String email,
    Optional<UUID> usuarioId,
    Optional<UUID> superUsuarioId,
    Set<String> roles
) {

    /**
     * Returns true if the actor has the given role in their token.
     *
     * @param role a Keycloak realm role string (e.g. "ADMIN", "USER")
     * @return true if the actor's roles include the given role
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Convenience: returns true if the actor has a superUsuarioId — meaning
     * they are associated with a SuperUsuario entity in the CRM.
     *
     * @return true if superUsuarioId is present
     */
    public boolean isSuperUsuario() {
        return superUsuarioId != null && superUsuarioId.isPresent();
    }
}