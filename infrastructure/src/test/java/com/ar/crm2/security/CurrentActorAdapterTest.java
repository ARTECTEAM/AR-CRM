package com.ar.crm2.security;

import com.ar.crm2.adapter.out.persistence.entity.RolEntity;
import com.ar.crm2.adapter.out.persistence.entity.UsuarioEntity;
import com.ar.crm2.adapter.out.persistence.repository.RolRepository;
import com.ar.crm2.adapter.out.persistence.repository.UsuarioRepository;
import com.ar.crm2.application.security.CurrentActor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentActorAdapterTest {
    private final UsuarioRepository users = mock(UsuarioRepository.class);
    private final RolRepository roles = mock(RolRepository.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesSubjectToActiveLocalRowsAndIgnoresForgedUsuarioIdClaim() {
        UUID localUserId = UUID.randomUUID();
        UUID forgedClaimId = UUID.randomUUID();
        UUID localRoleId = UUID.randomUUID();
        UsuarioEntity user = user(localUserId, localRoleId, true, "verified-sub");
        RolEntity role = role(localRoleId, true);
        when(users.findByKeycloakId("verified-sub")).thenReturn(Optional.of(user));
        when(roles.findById(localRoleId.toString())).thenReturn(Optional.of(role));
        authenticate("verified-sub", forgedClaimId);

        Optional<CurrentActor> actor = new CurrentActorAdapter(users, roles, "bootstrap-sub").currentActor();

        assertEquals(Optional.of(new CurrentActor(localUserId, localRoleId, false)), actor);
        verify(users).findByKeycloakId("verified-sub");
    }

    @Test
    void rejectsInactiveUserRoleDanglingRoleAndMalformedLocalIds() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        authenticate("subject", UUID.randomUUID());

        when(users.findByKeycloakId("subject")).thenReturn(Optional.of(user(userId, roleId, false, "subject")));
        assertTrue(new CurrentActorAdapter(users, roles, "").currentActor().isEmpty());
        verifyNoInteractions(roles);

        reset(users, roles);
        when(users.findByKeycloakId("subject")).thenReturn(Optional.of(user(userId, roleId, true, "subject")));
        when(roles.findById(roleId.toString())).thenReturn(Optional.of(role(roleId, false)));
        assertTrue(new CurrentActorAdapter(users, roles, "").currentActor().isEmpty());

        reset(users, roles);
        when(users.findByKeycloakId("subject")).thenReturn(Optional.of(user(userId, roleId, true, "subject")));
        when(roles.findById(roleId.toString())).thenReturn(Optional.empty());
        assertTrue(new CurrentActorAdapter(users, roles, "").currentActor().isEmpty());

        reset(users, roles);
        when(users.findByKeycloakId("subject")).thenReturn(Optional.of(
                UsuarioEntity.builder().id("not-a-uuid").rolId(roleId.toString()).activo(true).keycloakId("subject").build()));
        when(roles.findById(roleId.toString())).thenReturn(Optional.of(role(roleId, true)));
        assertTrue(new CurrentActorAdapter(users, roles, "").currentActor().isEmpty());
        reset(users, roles);
        when(users.findByKeycloakId("subject")).thenReturn(Optional.of(
                UsuarioEntity.builder().id(null).rolId(roleId.toString()).activo(true).keycloakId("subject").build()));
        when(roles.findById(roleId.toString())).thenReturn(Optional.of(role(roleId, true)));
        assertTrue(new CurrentActorAdapter(users, roles, "").currentActor().isEmpty());
    }

    @Test
    void onlyConfiguredExactSubjectReceivesBootstrapMarker() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(users.findByKeycloakId("subject")).thenReturn(Optional.of(user(userId, roleId, true, "subject")));
        when(roles.findById(roleId.toString())).thenReturn(Optional.of(role(roleId, true)));
        authenticate("subject", UUID.randomUUID());

        assertEquals(Optional.of(new CurrentActor(userId, roleId, true)),
                new CurrentActorAdapter(users, roles, "subject").currentActor());
        assertEquals(Optional.of(new CurrentActor(userId, roleId, false)),
                new CurrentActorAdapter(users, roles, "subject-other").currentActor());
    }

    @Test
    void rejectsMissingOrNonJwtAuthenticationAndDoesNotTrustClaimWhenLocalUserMissing() {
        UUID forgedClaimId = UUID.randomUUID();
        authenticate("unknown", forgedClaimId);
        when(users.findByKeycloakId("unknown")).thenReturn(Optional.empty());

        assertTrue(new CurrentActorAdapter(users, roles, "").currentActor().isEmpty());
        SecurityContextHolder.clearContext();
        assertTrue(new CurrentActorAdapter(users, roles, "").currentActor().isEmpty());
    }

    private static UsuarioEntity user(UUID id, UUID roleId, boolean active, String subject) {
        return UsuarioEntity.builder().id(id.toString()).nombre("User").correo("user@example.test")
                .rolId(roleId.toString()).creadoEn(LocalDateTime.now()).activo(active).keycloakId(subject).build();
    }

    private static RolEntity role(UUID id, boolean active) {
        return RolEntity.builder().id(id.toString()).nombre("Role").activo(active).build();
    }

    private static void authenticate(String subject, UUID usuarioIdClaim) {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").subject(subject)
                .claim("usuario_id", usuarioIdClaim.toString())
                .claim("realm_access", java.util.Map.of("roles", List.of("ADMIN"))).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of(), subject));
    }
}