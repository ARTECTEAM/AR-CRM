package com.ar.crm2.security;

import com.ar.crm2.application.security.ActorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;

/**
 * Unit tests for {@link KeycloakJwtActorContextMapper}.
 *
 * Verifies that Keycloak JWT claims are correctly mapped to the pure
 * application-layer {@link ActorContext}, and that malformed or missing
 * custom UUID claims fail gracefully to Optional.empty().
 *
 * These are pure unit tests — no Spring context, no Keycloak runtime.
 */
@ExtendWith(MockitoExtension.class)
class KeycloakJwtActorContextMapperTest {

    private KeycloakJwtActorContextMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new KeycloakJwtActorContextMapper();
    }

    // ------------------------------------------------------------------
    // Happy-path: all claims present and well-formed
    // ------------------------------------------------------------------

    @Test
    @DisplayName("map(Jwt) — all claims present — returns complete ActorContext")
    void map_jwt_allClaimsPresent_returnsFullActorContext() {
        // Arrange
        UUID usuarioId = UUID.randomUUID();
        UUID superUsuarioId = UUID.randomUUID();

        Jwt jwt = mockJwt("auth-subject-123", "testuser", "test@crm2.com",
                usuarioId.toString(), superUsuarioId.toString(),
                List.of("USER", "ADMIN"));

        // Act
        ActorContext ctx = mapper.map(jwt);

        // Assert
        assertEquals("auth-subject-123", ctx.subject());
        assertEquals("testuser", ctx.username());
        assertEquals("test@crm2.com", ctx.email());
        assertTrue(ctx.usuarioId().isEmpty());
        assertTrue(ctx.superUsuarioId().isPresent());
        assertEquals(superUsuarioId, ctx.superUsuarioId().get());
        assertTrue(ctx.hasRole("ADMIN"));
        assertTrue(ctx.hasRole("USER"));
        assertTrue(ctx.isSuperUsuario());
    }

    @Test
    @DisplayName("map(Jwt) — forged usuario_id is not trusted as a local CRM identity")
    void map_jwt_usuarioIdClaimIsNotTrusted() {
        UUID forgedUsuarioId = UUID.randomUUID();
        ActorContext actor = mapper.map(mockJwt("verified-sub", "user", null,
                forgedUsuarioId.toString(), null, List.of()));

        assertTrue(actor.usuarioId().isEmpty());
    }
    // ------------------------------------------------------------------
    // Sub-claim extraction
    // ------------------------------------------------------------------

    @Test
    @DisplayName("map(Authentication) — principal is Jwt — delegates to map(Jwt)")
    void map_authentication_principalIsJwt_delegatesToMapJwt() {
        UUID id = UUID.randomUUID();
        Jwt jwt = mockJwt("sub", "user", "u@c.com", id.toString(), null, List.of());
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);

        ActorContext ctx = mapper.map(auth);

        assertEquals("sub", ctx.subject());
        assertTrue(ctx.usuarioId().isEmpty());
    }

    @Test
    @DisplayName("map(Authentication) — principal is not Jwt — throws IllegalArgumentException")
    void map_authentication_principalNotJwt_throwsIllegalArgumentException() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("not-a-jwt");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> mapper.map(auth));
        assertTrue(ex.getMessage().contains("Unsupported principal type"));
        assertTrue(ex.getMessage().contains("java.lang.String"));
    }

    // ------------------------------------------------------------------
    // Missing optional claims → Optional.empty()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("map(Jwt) — usuario_id absent — usuarioId is Optional.empty()")
    void map_jwt_usuarioIdAbsent_usuarioIdEmpty() {
        Jwt jwt = mockJwt("sub", "user", "u@c.com", null, null, List.of());

        ActorContext ctx = mapper.map(jwt);

        assertTrue(ctx.usuarioId().isEmpty());
        assertFalse(ctx.isSuperUsuario());
    }

    @Test
    @DisplayName("map(Jwt) — super_usuario_id absent — superUsuarioId is Optional.empty()")
    void map_jwt_superUsuarioIdAbsent_superUsuarioIdEmpty() {
        UUID usuarioId = UUID.randomUUID();
        Jwt jwt = mockJwt("sub", "user", "u@c.com", usuarioId.toString(), null, List.of());

        ActorContext ctx = mapper.map(jwt);

        assertTrue(ctx.superUsuarioId().isEmpty());
        assertFalse(ctx.isSuperUsuario());
    }

    @Test
    @DisplayName("map(Jwt) — email absent — email is null")
    void map_jwt_emailAbsent_emailIsNull() {
        Jwt jwt = mockJwt("sub", "user", null, null, null, List.of());

        ActorContext ctx = mapper.map(jwt);

        assertNull(ctx.email());
    }

    // ------------------------------------------------------------------
    // Malformed UUID → Optional.empty() (fail-safe)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("map(Jwt) — usuario_id malformed UUID — returns Optional.empty()")
    void map_jwt_usuarioIdMalformed_returnsEmpty() {
        Jwt jwt = mockJwt("sub", "user", "u@c.com", "not-a-uuid", null, List.of());

        ActorContext ctx = mapper.map(jwt);

        assertTrue(ctx.usuarioId().isEmpty());
    }

    @Test
    @DisplayName("map(Jwt) — super_usuario_id malformed UUID — returns Optional.empty()")
    void map_jwt_superUsuarioIdMalformed_returnsEmpty() {
        UUID usuarioId = UUID.randomUUID();
        Jwt jwt = mockJwt("sub", "user", "u@c.com", usuarioId.toString(), "also-not-uuid", List.of());

        ActorContext ctx = mapper.map(jwt);

        assertTrue(ctx.superUsuarioId().isEmpty());
        assertFalse(ctx.isSuperUsuario());
    }

    @Test
    @DisplayName("map(Jwt) — usuario_id blank string — returns Optional.empty()")
    void map_jwt_usuarioIdBlank_returnsEmpty() {
        Jwt jwt = mockJwt("sub", "user", "u@c.com", "   ", null, List.of());

        ActorContext ctx = mapper.map(jwt);

        assertTrue(ctx.usuarioId().isEmpty());
    }

    // ------------------------------------------------------------------
    // Realm roles extraction
    // ------------------------------------------------------------------

    @Test
    @DisplayName("map(Jwt) — realm_access.roles populated — roles extracted correctly")
    void map_jwt_realmAccessRoles_extractsRoles() {
        Jwt jwt = mockJwtWithRealmAccess("sub", "user", null, null, List.of("ADMIN", "SUPPORT"));

        ActorContext ctx = mapper.map(jwt);

        assertEquals(Set.of("ADMIN", "SUPPORT"), ctx.roles());
        assertTrue(ctx.hasRole("ADMIN"));
        assertFalse(ctx.hasRole("USER"));
    }

    @Test
    @DisplayName("map(Jwt) — realm_access absent — roles empty set")
    void map_jwt_realmAccessAbsent_returnsEmptyRoles() {
        Jwt jwt = mockJwt("sub", "user", "u@c.com", null, null, List.of());

        ActorContext ctx = mapper.map(jwt);

        assertTrue(ctx.roles().isEmpty());
        assertFalse(ctx.hasRole("ANY"));
    }

    @Test
    @DisplayName("map(Jwt) — realm_access.roles contains non-strings — skipped silently")
    void map_jwt_realmAccessRolesWithNonStrings_skipsNonStrings() {
        Jwt jwt = mockJwtWithRealmAccessContainingNonStrings("sub", "user", null, null);

        ActorContext ctx = mapper.map(jwt);

        // Only the valid String "ADMIN" should be collected
        assertEquals(Set.of("ADMIN"), ctx.roles());
    }

    // ------------------------------------------------------------------
    // hasRole / isSuperUsuario convenience methods
    // ------------------------------------------------------------------

    @Test
    @DisplayName("hasRole — actor has role — returns true")
    void hasRole_actorHasRole_returnsTrue() {
        Jwt jwt = mockJwt("sub", "user", null, null, null, List.of("ROLE_USER"));
        ActorContext ctx = mapper.map(jwt);
        assertTrue(ctx.hasRole("ROLE_USER"));
    }

    @Test
    @DisplayName("hasRole — actor lacks role — returns false")
    void hasRole_actorLacksRole_returnsFalse() {
        Jwt jwt = mockJwt("sub", "user", null, null, null, List.of());
        ActorContext ctx = mapper.map(jwt);
        assertFalse(ctx.hasRole("ADMIN"));
    }

    @Test
    @DisplayName("hasRole — roles null — returns false (null-safe)")
    void hasRole_nullRoles_returnsFalse() {
        ActorContext ctx = new ActorContext("sub", "user", null,
                Optional.empty(), Optional.empty(), null);
        assertFalse(ctx.hasRole("ADMIN"));
    }

    // ------------------------------------------------------------------
    // Helper factories — build minimal mock Jwt with deep-stubs
    // ------------------------------------------------------------------

    /**
     * Builds a mock Jwt with the given claim values using deep-stubbing.
     * realm_access is a plain Map (not a nested structure).
     */
    private Jwt mockJwt(String sub, String username, String email,
                        String usuarioId, String superUsuarioId, List<String> roles) {
        Jwt jwt = mock(Jwt.class, withSettings().lenient());

        when(jwt.getClaimAsString("sub")).thenReturn(sub);
        when(jwt.getClaimAsString("preferred_username")).thenReturn(username);
        when(jwt.getClaimAsString("email")).thenReturn(email);
        when(jwt.getClaimAsString("usuario_id")).thenReturn(usuarioId);
        when(jwt.getClaimAsString("super_usuario_id")).thenReturn(superUsuarioId);

        // realm_access as a flat Map with "roles" key
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", roles);
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        return jwt;
    }

    /**
     * Builds a Jwt mock where realm_access.roles contains mixed types
     * (some String, some non-String) to test filtering.
     */
    @SuppressWarnings("unchecked")
    private Jwt mockJwtWithRealmAccessContainingNonStrings(String sub, String username,
                                                            String usuarioId, String superUsuarioId) {
        Jwt jwt = mock(Jwt.class, withSettings().lenient());
        when(jwt.getClaimAsString("sub")).thenReturn(sub);
        when(jwt.getClaimAsString("preferred_username")).thenReturn(username);
        when(jwt.getClaimAsString("email")).thenReturn(null);
        when(jwt.getClaimAsString("usuario_id")).thenReturn(usuarioId);
        when(jwt.getClaimAsString("super_usuario_id")).thenReturn(superUsuarioId);

        // Mixed: String "ADMIN" + Integer 1 (non-String, should be skipped)
        List<Object> mixedRoles = List.of("ADMIN", 1);
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", mixedRoles);
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        return jwt;
    }

    private Jwt mockJwtWithRealmAccess(String sub, String username, String email,
                                       String usuarioId, List<String> roles) {
        Jwt jwt = mock(Jwt.class, withSettings().lenient());

        when(jwt.getClaimAsString("sub")).thenReturn(sub);
        when(jwt.getClaimAsString("preferred_username")).thenReturn(username);
        when(jwt.getClaimAsString("email")).thenReturn(email);
        when(jwt.getClaimAsString("usuario_id")).thenReturn(usuarioId);
        when(jwt.getClaimAsString("super_usuario_id")).thenReturn(null);

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", new ArrayList<>(roles));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        return jwt;
    }
}