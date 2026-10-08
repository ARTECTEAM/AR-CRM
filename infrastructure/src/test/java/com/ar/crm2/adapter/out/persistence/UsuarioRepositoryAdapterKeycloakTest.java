package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.UsuarioEntity;
import com.ar.crm2.adapter.out.persistence.mapper.UsuarioMapper;
import com.ar.crm2.adapter.out.persistence.RoleManagerGovernance;
import com.ar.crm2.adapter.out.persistence.repository.UsuarioRepository;
import com.ar.crm2.model.entity.Usuario;
import com.ar.crm2.model.vo.RolId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UsuarioRepositoryAdapter} keycloakId behaviors.
 * Verifies findByKeycloakId delegation and keycloakId preservation in save flow.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioRepositoryAdapterKeycloakTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private RoleManagerGovernance governance;

    @InjectMocks
    private UsuarioRepositoryAdapter adapter;

    private static final String NOMBRE = "Juan Perez";
    private static final String CORREO = "juan@example.com";
    private static final String KEYCLOAK_ID = "kc-uuid-abc123";
    private static final LocalDateTime AHORA = LocalDateTime.now();

    // ── findByKeycloakId ───────────────────────────────────────────────

    @Nested
    @DisplayName("findByKeycloakId()")
    class FindByKeycloakId {

        @Test
        @DisplayName("returns empty when no Usuario with that keycloakId exists")
        void findByKeycloakId_notFound_returnsEmpty() {
            when(repository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());

            Optional<Usuario> result = adapter.findByKeycloakId(KEYCLOAK_ID);

            assertTrue(result.isEmpty());
            verify(repository).findByKeycloakId(KEYCLOAK_ID);
        }

        @Test
        @DisplayName("returns Usuario domain when found by keycloakId")
        void findByKeycloakId_found_returnsMappedDomain() {
            String id = UUID.randomUUID().toString();
            UsuarioEntity entity = UsuarioEntity.builder()
                    .id(id)
                    .nombre(NOMBRE)
                    .correo(CORREO)
                    .rolId(UUID.randomUUID().toString())
                    .creadoEn(AHORA)
                    .activo(true)
                    .keycloakId(KEYCLOAK_ID)
                    .build();

            when(repository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(entity));

            Optional<Usuario> result = adapter.findByKeycloakId(KEYCLOAK_ID);

            assertTrue(result.isPresent());
            assertEquals(KEYCLOAK_ID, result.get().getKeycloakId());
            verify(repository).findByKeycloakId(KEYCLOAK_ID);
        }

        @Test
        @DisplayName("repository is called with exact keycloakId value")
        void findByKeycloakId_delegatesWithCorrectParameter() {
            String specificKcid = "specific-keycloak-id-xyz";
            when(repository.findByKeycloakId(specificKcid)).thenReturn(Optional.empty());

            adapter.findByKeycloakId(specificKcid);

            verify(repository).findByKeycloakId(specificKcid);
        }
    }

    // ── save preserves keycloakId ────────────────────────────────────

    @Nested
    @DisplayName("save() preserves keycloakId")
    class SavePreservesKeycloakId {

        @Test
        @DisplayName("save with keycloakId maps it to entity and back to domain")
        void save_conKeycloakId_roundTrips() {
            UsuarioId usuarioId = UsuarioId.create();
            Usuario domain = Usuario.reconstitute(
                    usuarioId,
                    NOMBRE,
                    CORREO,
                    RolId.create(),
                    AHORA,
                    true,
                    KEYCLOAK_ID
            );

            String id = UUID.randomUUID().toString();
            UsuarioEntity savedEntity = UsuarioEntity.builder()
                    .id(id)
                    .nombre(NOMBRE)
                    .correo(CORREO)
                    .rolId(UUID.randomUUID().toString())
                    .creadoEn(AHORA)
                    .activo(true)
                    .keycloakId(KEYCLOAK_ID)
                    .build();

            when(repository.save(any(UsuarioEntity.class))).thenReturn(savedEntity);
            when(governance.lockRoles()).thenReturn(List.of());
            when(repository.findById(usuarioId.value().toString())).thenReturn(Optional.empty());

            Usuario result = adapter.save(domain);

            assertEquals(KEYCLOAK_ID, result.getKeycloakId());
            verify(repository).save(any(UsuarioEntity.class));
        }

        @Test
        @DisplayName("save without keycloakId maps null through entity")
        void save_sinKeycloakId_roundTripsNull() {
            UsuarioId usuarioId = UsuarioId.create();
            Usuario domain = Usuario.reconstitute(
                    usuarioId,
                    NOMBRE,
                    CORREO,
                    RolId.create(),
                    AHORA,
                    true,
                    null
            );

            String id = UUID.randomUUID().toString();
            UsuarioEntity savedEntity = UsuarioEntity.builder()
                    .id(id)
                    .nombre(NOMBRE)
                    .correo(CORREO)
                    .rolId(UUID.randomUUID().toString())
                    .creadoEn(AHORA)
                    .activo(true)
                    .keycloakId(null)
                    .build();

            when(repository.save(any(UsuarioEntity.class))).thenReturn(savedEntity);
            when(governance.lockRoles()).thenReturn(List.of());
            when(repository.findById(usuarioId.value().toString())).thenReturn(Optional.empty());

            Usuario result = adapter.save(domain);

            assertNull(result.getKeycloakId());
        }
    }

    @Test
    @DisplayName("delete checks manager governance under a transaction before deleting")
    void delete_checksManagerGovernanceInsideTransaction() throws Exception {
        UUID userId = UUID.randomUUID();
        UsuarioId usuarioId = UsuarioId.from(userId);
        String roleId = UUID.randomUUID().toString();
        UsuarioEntity existing = UsuarioEntity.builder()
                .id(userId.toString())
                .nombre(NOMBRE)
                .correo(CORREO)
                .rolId(roleId)
                .creadoEn(AHORA)
                .activo(true)
                .keycloakId(KEYCLOAK_ID)
                .build();
        when(governance.lockRoles()).thenReturn(List.of());
        when(repository.findById(userId.toString())).thenReturn(Optional.of(existing));

        adapter.deleteById(usuarioId);

        InOrder inOrder = inOrder(governance, repository);
        inOrder.verify(governance).lockRoles();
        inOrder.verify(repository).findById(userId.toString());
        inOrder.verify(governance).assertUserRemoval(eq(List.of()), any(Usuario.class));
        inOrder.verify(repository).deleteById(userId.toString());
        assertNotNull(UsuarioRepositoryAdapter.class.getMethod("deleteById", UsuarioId.class)
                .getAnnotation(Transactional.class));
    }
}
