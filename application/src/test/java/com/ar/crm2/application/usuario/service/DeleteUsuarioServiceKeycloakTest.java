package com.ar.crm2.application.usuario.service;

import com.ar.crm2.application.identity.port.out.DeleteIdentityPort;
import com.ar.crm2.application.usuario.command.DeleteUsuarioCommand;
import com.ar.crm2.application.usuario.exception.UsuarioNotFoundException;
import com.ar.crm2.application.usuario.port.out.DeleteUsuarioByIdPort;
import com.ar.crm2.application.usuario.port.out.FindUsuarioByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.CurrentActor;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeleteUsuarioService} Keycloak deletion flow.
 * <p>
 * Covers: local delete runs before the best-effort Keycloak delete, the
 * Keycloak cleanup is skipped when there is no {@code keycloakId}, Keycloak
 * failures are swallowed so the local delete still stands, and missing
 * entities surface {@link UsuarioNotFoundException} without touching either
 * store.
 */
@ExtendWith(MockitoExtension.class)
class DeleteUsuarioServiceKeycloakTest {

    private static final String NOMBRE = "Juan Perez";
    private static final String CORREO = "juan@example.com";
    private static final String KEYCLOAK_ID = "kc-uuid-delete-test-789";
    private static final LocalDateTime AHORA = LocalDateTime.now();

    @Mock
    private FindUsuarioByIdPort findPort;

    @Mock
    private DeleteUsuarioByIdPort deletePort;

    @Mock
    private DeleteIdentityPort deleteIdentityPort;

    @Mock
    private CrmAuthorization authorization;

    @Mock
    private CurrentActorPort currentActorPort;

    @Mock
    private AuthorizationMutationPort mutationPort;

    @InjectMocks
    private DeleteUsuarioService service;

    @BeforeEach
    void configureAuthorizedActor() {
        when(mutationPort.execute(any())).thenAnswer(invocation ->
                ((java.util.function.Supplier<?>) invocation.getArgument(0)).get());
        when(currentActorPort.currentActor()).thenReturn(Optional.of(
                new CurrentActor(UUID.randomUUID(), UUID.randomUUID(), false)));
    }

    // ── Happy path ─────────────────────────────────────────────────

    @Nested
    @DisplayName("delete removes local row first, then Keycloak user")
    class DeleteExitoso {

        @Test
        @DisplayName("deletes local record first, then deletes Keycloak user")
        void delete_exitoso_eliminaLocalAntesDeKeycloak() {
            // Given
            UUID id = UUID.randomUUID();
            UsuarioId usuarioId = UsuarioId.from(id);
            Usuario existing = Usuario.reconstitute(
                usuarioId, NOMBRE, CORREO, RolId.create(),
                AHORA.minusDays(1), true, KEYCLOAK_ID
            );
            when(findPort.findById(usuarioId)).thenReturn(Optional.of(existing));

            DeleteUsuarioCommand cmd = new DeleteUsuarioCommand(id);

            // When
            service.delete(cmd);

            // Then — local delete is invoked before Keycloak delete
            InOrder inOrder = inOrder(findPort, deletePort, deleteIdentityPort);
            inOrder.verify(findPort).findById(usuarioId);
            inOrder.verify(deletePort).deleteById(usuarioId);
            inOrder.verify(deleteIdentityPort).delete(KEYCLOAK_ID);
        }

        @Test
        @DisplayName("deletes local record even when keycloakId is null")
        void delete_sinKeycloakId_eliminaLocalSolamente() {
            // Given
            UUID id = UUID.randomUUID();
            UsuarioId usuarioId = UsuarioId.from(id);
            Usuario existing = Usuario.reconstitute(
                usuarioId, NOMBRE, CORREO, RolId.create(),
                AHORA.minusDays(1), true, null
            );
            when(findPort.findById(usuarioId)).thenReturn(Optional.of(existing));

            DeleteUsuarioCommand cmd = new DeleteUsuarioCommand(id);

            // When
            service.delete(cmd);

            // Then — no Keycloak call, only local delete
            verify(deleteIdentityPort, never()).delete(any());
            verify(deletePort).deleteById(usuarioId);
        }
    }

    // ── Keycloak delete failure (best-effort) ──────────────────────

    @Nested
    @DisplayName("Keycloak delete fails after local delete — best-effort")
    class KeycloakDeleteFalla {

        @Test
        @DisplayName("swallows Keycloak delete failure and returns success after local delete")
        void delete_keycloakFalla_eliminaLocalYContinua() {
            // Given
            UUID id = UUID.randomUUID();
            UsuarioId usuarioId = UsuarioId.from(id);
            Usuario existing = Usuario.reconstitute(
                usuarioId, NOMBRE, CORREO, RolId.create(),
                AHORA.minusDays(1), true, KEYCLOAK_ID
            );
            when(findPort.findById(usuarioId)).thenReturn(Optional.of(existing));
            doThrow(new RuntimeException("Keycloak unreachable"))
                .when(deleteIdentityPort).delete(KEYCLOAK_ID);

            DeleteUsuarioCommand cmd = new DeleteUsuarioCommand(id);

            // When / Then — local delete still happens, no exception surfaces
            assertDoesNotThrow(() -> service.delete(cmd));
            verify(deletePort).deleteById(usuarioId);
            verify(deleteIdentityPort).delete(KEYCLOAK_ID);
        }
    }

    // ── Local delete failure ───────────────────────────────────────

    @Nested
    @DisplayName("local delete fails — Keycloak must NOT be touched")
    class LocalDeleteFalla {

        @Test
        @DisplayName("does not call Keycloak when local delete throws")
        void delete_localFalla_noLlamaKeycloak() {
            // Given
            UUID id = UUID.randomUUID();
            UsuarioId usuarioId = UsuarioId.from(id);
            Usuario existing = Usuario.reconstitute(
                usuarioId, NOMBRE, CORREO, RolId.create(),
                AHORA.minusDays(1), true, KEYCLOAK_ID
            );
            when(findPort.findById(usuarioId)).thenReturn(Optional.of(existing));
            doThrow(new RuntimeException("DB constraint violation"))
                .when(deletePort).deleteById(usuarioId);

            DeleteUsuarioCommand cmd = new DeleteUsuarioCommand(id);

            // When / Then
            assertThrows(RuntimeException.class, () -> service.delete(cmd));
            verify(deleteIdentityPort, never()).delete(any());
        }
    }

    // ── Not found ─────────────────────────────────────────────────

    @Nested
    @DisplayName("usuario not found")
    class UsuarioNoEncontrado {

        @Test
        @DisplayName("throws UsuarioNotFoundException when entity does not exist")
        void delete_entidadNoExistente_lanzaExcepcion() {
            // Given
            UUID id = UUID.randomUUID();
            UsuarioId usuarioId = UsuarioId.from(id);
            when(findPort.findById(usuarioId)).thenReturn(Optional.empty());

            DeleteUsuarioCommand cmd = new DeleteUsuarioCommand(id);

            // When / Then
            assertThrows(UsuarioNotFoundException.class, () -> service.delete(cmd));
            verify(deletePort, never()).deleteById(any());
            verify(deleteIdentityPort, never()).delete(any());
        }
    }
}
