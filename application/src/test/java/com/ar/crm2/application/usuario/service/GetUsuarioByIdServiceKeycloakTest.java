package com.ar.crm2.application.usuario.service;

import com.ar.crm2.application.usuario.command.GetUsuarioByIdCommand;
import com.ar.crm2.application.usuario.exception.UsuarioNotFoundException;
import com.ar.crm2.application.usuario.port.in.GetUsuarioByIdUseCase;
import com.ar.crm2.application.usuario.port.out.FindUsuarioByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.entity.Usuario;
import com.ar.crm2.model.vo.RolId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GetUsuarioByIdService} retrieval path — verifying keycloakId
 * is correctly retrieved and exposed through the service layer (not just at mapper level).
 */
@ExtendWith(MockitoExtension.class)
class GetUsuarioByIdServiceKeycloakTest {

    private static final String NOMBRE = "Juan Perez";
    private static final String CORREO = "juan@example.com";
    private static final String KEYCLOAK_ID = "kc-uuid-retrieval-test-123";
    private static final LocalDateTime AHORA = LocalDateTime.now();

    @Mock
    private FindUsuarioByIdPort findPort;

    @Mock
    private CrmAuthorization authorization;

    @InjectMocks
    private GetUsuarioByIdService service;

    // ── retrieval with keycloakId ──────────────────────────────────

    @Nested
    @DisplayName("getById() retrieves keycloakId correctly")
    class RecuperaKeycloakId {

        @Test
        @DisplayName("returns Usuario with keycloakId when entity has one linked")
        void getById_conKeycloakId_retornaConKeycloakId() {
            UUID id = UUID.randomUUID();
            UsuarioId usuarioId = UsuarioId.from(id);
            Usuario existing = Usuario.reconstitute(
                    usuarioId, NOMBRE, CORREO, RolId.create(),
                    AHORA.minusDays(1), true, KEYCLOAK_ID
            );
            when(findPort.findById(usuarioId)).thenReturn(Optional.of(existing));

            GetUsuarioByIdCommand cmd = new GetUsuarioByIdCommand(id);
            Usuario result = service.getById(cmd);

            assertEquals(KEYCLOAK_ID, result.getKeycloakId());
            assertEquals(NOMBRE, result.getNombre());
            assertEquals(CORREO, result.getCorreo());
        }

        @Test
        @DisplayName("returns Usuario with null keycloakId when entity has none")
        void getById_sinKeycloakId_retornaConKeycloakIdNulo() {
            UUID id = UUID.randomUUID();
            UsuarioId usuarioId = UsuarioId.from(id);
            Usuario existing = Usuario.reconstitute(
                    usuarioId, NOMBRE, CORREO, RolId.create(),
                    AHORA.minusDays(1), true, null
            );
            when(findPort.findById(usuarioId)).thenReturn(Optional.of(existing));

            GetUsuarioByIdCommand cmd = new GetUsuarioByIdCommand(id);
            Usuario result = service.getById(cmd);

            assertNull(result.getKeycloakId());
        }

        @Test
        @DisplayName("throws UsuarioNotFoundException when entity not found")
        void getById_entidadNoExistente_lanzaExcepcion() {
            UUID id = UUID.randomUUID();
            UsuarioId usuarioId = UsuarioId.from(id);
            when(findPort.findById(usuarioId)).thenReturn(Optional.empty());

            GetUsuarioByIdCommand cmd = new GetUsuarioByIdCommand(id);

            assertThrows(UsuarioNotFoundException.class, () -> service.getById(cmd));
        }
    }
}
