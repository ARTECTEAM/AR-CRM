package com.ar.crm2.security;

import com.ar.crm2.application.security.ActorContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Maps a Spring Security {@link Jwt} (validated by the OAuth2 Resource Server)
 * into a pure application-layer {@link ActorContext}.
 *
 * This component lives in the infrastructure layer — Spring Security types
 * are confined here and never leak into application/domain.
 *
 * Keycloak claim names used:
 * - {@code sub}             → subject
 * - {@code preferred_username} → username
 * - {@code email}           → email
 * - CRM usuario identity is resolved from the active local user by JWT subject
 * - {@code super_usuario_id} → superUsuarioId (custom Keycloak mapper, UUID as string)
 * - {@code realm_access.roles} → roles (array from Keycloak token)
 *
 * Missing or malformed custom UUID claims result in empty Optionals — the
 * actor context is still valid but identity fields will be absent.
 */
@Component
public class  KeycloakJwtActorContextMapper {

    private static final String CLAIM_SUB = "sub";
    private static final String CLAIM_PREFERRED_USERNAME = "preferred_username";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_SUPER_USUARIO_ID = "super_usuario_id";
    private static final String CLAIM_REALM_ACCESS = "realm_access";
    private static final String CLAIM_ROLES = "roles";

    /**
     * Maps the authenticated {@link Jwt} from the Spring Security context
     * into an {@link ActorContext}.
     *
     * @param jwt the validated JWT (never null when called from a authenticated request)
     * @return an immutable ActorContext with all available claims
     */
    public ActorContext map(Jwt jwt) {
        String subject = jwt.getClaimAsString(CLAIM_SUB);
        String username = jwt.getClaimAsString(CLAIM_PREFERRED_USERNAME);
        String email = jwt.getClaimAsString(CLAIM_EMAIL);
        Optional<UUID> usuarioId = Optional.empty();
        Optional<UUID> superUsuarioId = parseOptionalUuid(jwt, CLAIM_SUPER_USUARIO_ID);
        Set<String> roles = extractRoles(jwt);

        return new ActorContext(subject, username, email, usuarioId, superUsuarioId, roles);
    }

    /**
     * Overload: extract ActorContext from a generic {@link Authentication}.
     * Falls back to the JWT claim set when the authentication principal is a JWT.
     *
     * @param authentication the Spring Security Authentication (must contain a Jwt principal)
     * @return ActorContext derived from the authentication's JWT
     * @throws IllegalArgumentException if the principal is not a Jwt instance
     */
    public ActorContext map(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return map(jwt);
        }
        throw new IllegalArgumentException(
            "Unsupported principal type: " + principal.getClass().getName()
                + ". Expected org.springframework.security.oauth2.jwt.Jwt."
        );
    }

    private Optional<UUID> parseOptionalUuid(Jwt jwt, String claimName) {
        String value = jwt.getClaimAsString(claimName);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            // Malformed UUID — treat as absent rather than failing
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(CLAIM_REALM_ACCESS);
        if (realmAccess == null) {
            return Collections.emptySet();
        }
        Object rolesObj = realmAccess.get(CLAIM_ROLES);
        if (rolesObj instanceof List<?> rawRoles) {
            Set<String> roles = new HashSet<>();
            for (Object r : rawRoles) {
                if (r instanceof String role) {
                    roles.add(role);
                }
            }
            return Collections.unmodifiableSet(roles);
        }
        return Collections.emptySet();
    }
}