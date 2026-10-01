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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentActorAdapterTest {
    private final UsuarioRepository users = mock(UsuarioRepository.class);
    private final RolRepository roles = mock(RolRepository.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesSubToActiveLocalRowsAndIgnoresCustomUsuarioClaim() {
        UUID localUserId = UUID.randomUUID();
        UUID staleClaimId = UUID.randomUUID();
        UUID localRoleId = UUID.randomUUID();
        UsuarioEntity user = UsuarioEntity.builder().id(localUserId.toString()).nombre("User")
                .correo("user@example.test").rolId(localRoleId.toString())
                .creadoEn(java.time.LocalDateTime.now()).activo(true).keycloakId("verified-sub").build();
        RolEntity role = RolEntity.builder().id(localRoleId.toString()).nombre("No grants")
                .activo(true).build();
        when(users.findByKeycloakId("verified-sub")).thenReturn(Optional.of(user));
        when(roles.findById(localRoleId.toString())).thenReturn(Optional.of(role));
        authenticate("verified-sub", staleClaimId);

        Optional<CurrentActor> actor = new CurrentActorAdapter(users, roles, "operator-sub").currentActor();

        assertThat(actor).contains(new CurrentActor(localUserId, localRoleId, false));
    }

    @Test
    void bootstrapSubjectMatchesExactSubOnlyAndInactiveLocalActorsAreRejected() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UsuarioEntity user = UsuarioEntity.builder().id(userId.toString()).nombre("User")
                .correo("user@example.test").rolId(roleId.toString())
                .creadoEn(java.time.LocalDateTime.now()).activo(true).keycloakId("operator-sub").build();
        RolEntity role = RolEntity.builder().id(roleId.toString()).nombre("Role").activo(true).build();
        when(users.findByKeycloakId("operator-sub")).thenReturn(Optional.of(user));
        when(roles.findById(roleId.toString())).thenReturn(Optional.of(role));
        authenticate("operator-sub", UUID.randomUUID());

        assertThat(new CurrentActorAdapter(users, roles, "operator-sub").currentActor())
                .contains(new CurrentActor(userId, roleId, true));
        assertThat(new CurrentActorAdapter(users, roles, "other-sub").currentActor())
                .contains(new CurrentActor(userId, roleId, false));

        user.setActivo(false);
        assertThat(new CurrentActorAdapter(users, roles, "operator-sub").currentActor()).isEmpty();
    }

    private static void authenticate(String subject, UUID usuarioIdClaim) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .claim("usuario_id", usuarioIdClaim.toString())
                .claim("realm_access", java.util.Map.of("roles", List.of("ADMIN")))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of(), subject));
    }
}
