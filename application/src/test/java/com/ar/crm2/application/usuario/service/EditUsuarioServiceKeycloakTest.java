package com.ar.crm2.application.usuario.service;

import com.ar.crm2.application.identity.port.out.SetIdentityEnabledPort;
import com.ar.crm2.application.identity.port.out.SyncIdentityEmailPort;
import com.ar.crm2.application.usuario.command.EditUsuarioCommand;
import com.ar.crm2.application.usuario.exception.UsuarioNotFoundException;
import com.ar.crm2.application.usuario.port.in.EditUsuarioUseCase;
import com.ar.crm2.application.usuario.port.out.FindUsuarioByIdPort;
import com.ar.crm2.application.usuario.port.out.SaveUsuarioPort;
import com.ar.crm2.application.usuario.port.out.PromoteBootstrapAdministratorPort;
import com.ar.crm2.application.rol.port.out.FindRolByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.application.security.port.out.AuthorizationMutationPort;
import com.ar.crm2.model.entity.Usuario;
import com.ar.crm2.model.vo.RolId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for immutable Keycloak identity-link semantics during user profile edits.
 */
@ExtendWith(MockitoExtension.class)
class EditUsuarioServiceKeycloakTest {

    private static final String NOMBRE = "Juan Perez";
    private static final String CORREO = "juan@example.com";
    private static final String KEYCLOAK_ID_EXISTING = "existing-kc-uuid-abc";
    private static final String KEYCLOAK_ID_NEW = "new-kc-uuid-xyz";
    private static final LocalDateTime AHORA = LocalDateTime.now();

    @Mock
    private FindUsuarioByIdPort findPort;

    @Mock
    private SaveUsuarioPort savePort;

    @Mock
    private SyncIdentityEmailPort syncEmailPort;

    @Mock
    private SetIdentityEnabledPort setEnabledPort;

    @Mock
    private CrmAuthorization authorization;

    @Mock
    private CurrentActorPort currentActorPort;

    @Mock
    private FindRolByIdPort findRolByIdPort;

    @Mock
    private PromoteBootstrapAdministratorPort promoteBootstrapAdministratorPort;

    @Mock
    private AuthorizationMutationPort mutationPort;

    @InjectMocks
    private EditUsuarioService service;

    @BeforeEach
    void configureMutationPort() {
        lenient().when(mutationPort.execute(any())).thenAnswer(invocation ->
                ((java.util.function.Supplier<?>) invocation.getArgument(0)).get());
    }

    private void setAuthenticatedActor() {
        when(currentActorPort.currentActor()).thenReturn(Optional.of(
                new CurrentActor(UUID.randomUUID(), UUID.randomUUID(), false)));
    }

    private EditUsuarioCommand buildCommand(UUID id, String nombre, String correo,
                                            UUID rolId, String keycloakId) {
        return new EditUsuarioCommand(id, nombre, correo, rolId, keycloakId);
    }

    // ── keycloakId PRESERVE semantics (command.keycloakId == null) ─────

    @Nested
    @DisplayName("keycloakId preserve — command.keycloakId() is null")
    class PreservaKeycloakId {

        @Test
        @DisplayName("preserves existing keycloakId when command.keycloakId() is null")
        void edit_conKeycloakIdNulo_preservaExistente() {
            UUID id = UUID.randomUUID();
            UsuarioId usuarioId = UsuarioId.from(id);
            Usuario existing = Usuario.reconstitute(
                    usuarioId, NOMBRE, CORREO, RolId.create(),
                    AHORA.minusDays(1), true, KEYCLOAK_ID_EXISTING
            );
            when(findPort.findById(usuarioId)).thenReturn(Optional.of(existing));
            setAuthenticatedActor();

            EditUsuarioCommand cmd = buildCommand(
                    id, NOMBRE + " Updated", CORREO,
                    existing.getRolId().value(), null
            );

            service.edit(cmd);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(savePort).save(captor.capture());
            assertEquals(KEYCLOAK_ID_EXISTING, captor.getValue().getKeycloakId());
        }

        @Test
        @DisplayName("preserves null keycloakId when existing has none and command omits it")
        void edit_sinKeycloakIdExistente_preservaNull() {
            UUID id = UUID.randomUUID();
            UsuarioId usuarioId = UsuarioId.from(id);
            Usuario existing = Usuario.reconstitute(
                    usuarioId, NOMBRE, CORREO, RolId.create(),
                    AHORA.minusDays(1), true, null
            );
            when(findPort.findById(usuarioId)).thenReturn(Optional.of(existing));
            setAuthenticatedActor();

            EditUsuarioCommand cmd = buildCommand(
                    id, NOMBRE + " Updated", CORREO,
                    existing.getRolId().value(), null
            );

            service.edit(cmd);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(savePort).save(captor.capture());
            assertNull(captor.getValue().getKeycloakId());
        }
    }

    // ── keycloakId immutability semantics (command.keycloakId != null) ──────

    @Nested
    @DisplayName("keycloakId must match existing linkage when provided")
    class ActualizaKeycloakId {

        @Test
        @DisplayName("rejects a different keycloakId")
        void edit_conKeycloakIdNuevo_rechazaCambio() {
            UUID id = UUID.randomUUID();
            UsuarioId usuarioId = UsuarioId.from(id);
            Usuario existing = Usuario.reconstitute(
                    usuarioId, NOMBRE, CORREO, RolId.create(),
                    AHORA.minusDays(1), true, KEYCLOAK_ID_EXISTING
            );
            when(findPort.findById(usuarioId)).thenReturn(Optional.of(existing));
            setAuthenticatedActor();

            EditUsuarioCommand cmd = buildCommand(
                    id, NOMBRE + " Updated", CORREO,
                    existing.getRolId().value(), KEYCLOAK_ID_NEW
            );

            assertThrows(CrmAuthorizationDeniedException.class, () -> service.edit(cmd));
            verify(savePort, never()).save(any());
        }

        @Test
        @DisplayName("rejects a blank keycloakId rather than clearing linkage")
        void edit_conKeycloakIdBlank_rechazaCambio() {
            UUID id = UUID.randomUUID();
            UsuarioId usuarioId = UsuarioId.from(id);
            Usuario existing = Usuario.reconstitute(
                    usuarioId, NOMBRE, CORREO, RolId.create(),
                    AHORA.minusDays(1), true, KEYCLOAK_ID_EXISTING
            );
            when(findPort.findById(usuarioId)).thenReturn(Optional.of(existing));
            setAuthenticatedActor();

            EditUsuarioCommand cmd = buildCommand(
                    id, NOMBRE + " Updated", CORREO,
                    existing.getRolId().value(), "   "
            );

            assertThrows(CrmAuthorizationDeniedException.class, () -> service.edit(cmd));
            verify(savePort, never()).save(any());
        }
    }

    // ── aggregate identity preservation ───────────────────────────────

    @Nested
    @DisplayName("aggregate identity fields are preserved from existing")
    class PreservaIdentidad {

        @Test
        @DisplayName("preserves existing id, rolId, creadoEn, activo regardless of command values")
        void edit_preservaCamposDeIdentidad() {
            UUID id = UUID.randomUUID();
            UsuarioId usuarioId = UsuarioId.from(id);
            LocalDateTime originalCreadoEn = AHORA.minusDays(5);
            Usuario existing = Usuario.reconstitute(
                    usuarioId, NOMBRE, CORREO, RolId.create(),
                    originalCreadoEn, true, KEYCLOAK_ID_EXISTING
            );
            when(findPort.findById(usuarioId)).thenReturn(Optional.of(existing));
            setAuthenticatedActor();

            EditUsuarioCommand cmd = buildCommand(
                    id, NOMBRE + " Updated", CORREO + ".ar",
                    existing.getRolId().value(), KEYCLOAK_ID_EXISTING
            );

            service.edit(cmd);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(savePort).save(captor.capture());
            Usuario saved = captor.getValue();
            assertEquals(usuarioId, saved.getId());
            assertEquals(existing.getRolId(), saved.getRolId());
            assertEquals(originalCreadoEn, saved.getCreadoEn());
            assertTrue(saved.isActivo());
            assertEquals(KEYCLOAK_ID_EXISTING, saved.getKeycloakId());
        }

        @Test
        @DisplayName("throws UsuarioNotFoundException when entity does not exist")
        void edit_conEntidadInexistente_lanzaExcepcion() {
            UUID id = UUID.randomUUID();
            UsuarioId usuarioId = UsuarioId.from(id);
            when(findPort.findById(usuarioId)).thenReturn(Optional.empty());

            EditUsuarioCommand cmd = buildCommand(
                    id, NOMBRE + " Updated", CORREO,
                    UUID.randomUUID(), KEYCLOAK_ID_NEW
            );

            assertThrows(UsuarioNotFoundException.class, () -> service.edit(cmd));
            verify(savePort, never()).save(any());
        }
    }
}
