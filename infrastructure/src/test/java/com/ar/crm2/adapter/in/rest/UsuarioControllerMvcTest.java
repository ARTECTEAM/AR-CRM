package com.ar.crm2.adapter.in.rest;

import com.ar.crm2.application.identity.model.IdentityProvisioningException;
import com.ar.crm2.application.usuario.command.CreateUsuarioCommand;
import com.ar.crm2.application.usuario.command.DeleteUsuarioCommand;
import com.ar.crm2.application.usuario.command.EditUsuarioCommand;
import com.ar.crm2.application.usuario.port.in.CreateUsuarioUseCase;
import com.ar.crm2.application.usuario.port.in.DeleteUsuarioUseCase;
import com.ar.crm2.application.usuario.port.in.EditUsuarioUseCase;
import com.ar.crm2.application.usuario.port.in.ForgotPasswordUseCase;
import com.ar.crm2.application.usuario.port.in.GetAllUsuariosUseCase;
import com.ar.crm2.application.usuario.port.in.GetUsuarioByIdUseCase;
import com.ar.crm2.application.usuario.port.in.RequestPasswordChangeUseCase;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Usuario;
import com.ar.crm2.model.vo.RolId;
import com.ar.crm2.model.vo.UsuarioId;
import com.ar.crm2.security.KeycloakJwtActorContextMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Real MockMvc tests for UsuarioController REST endpoints.
 *
 * Uses @WebMvcTest to load the Spring MVC slice with real JSON serialization,
 * validation, controller dispatching, and GlobalExceptionHandler.
 *
 * @WithMockUser bypasses Spring Security so requests reach the controller.
 * Use cases are @MockitoBean to avoid real DB/Keycloak.
 */
@WebMvcTest(controllers = {UsuarioController.class, GlobalExceptionHandler.class})
@WithMockUser
class UsuarioControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateUsuarioUseCase createUseCase;

    @MockitoBean
    private GetAllUsuariosUseCase getAllUseCase;

    @MockitoBean
    private GetUsuarioByIdUseCase getByIdUseCase;

    @MockitoBean
    private EditUsuarioUseCase editUseCase;

    @MockitoBean
    private DeleteUsuarioUseCase deleteUseCase;

    @MockitoBean
    private RequestPasswordChangeUseCase requestPasswordChangeUseCase;

    @MockitoBean
    private ForgotPasswordUseCase forgotPasswordUseCase;

    @MockitoBean
    private KeycloakJwtActorContextMapper actorContextMapper;

    @MockitoBean
    private CurrentActorPort currentActorPort;

    @MockitoBean
    private CrmAuthorization authorization;

    @BeforeEach
    void allowExpectedSensitiveFieldsInResponseFixtures() {
        when(currentActorPort.currentActor()).thenReturn(Optional.of(
                new CurrentActor(UUID.randomUUID(), UUID.randomUUID(), false)));
        when(authorization.fieldPolicy(RecursoCrm.USUARIO)).thenReturn(new ResourceReadPolicy(
                AlcanceCrm.TODO_COMPARTIDO,
                Set.of(GrupoCampoSensible.CONTACTO_PRIVADO),
                Set.of(),
                Set.of()));
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private static final String NOMBRE = "Juan Perez";
    private static final String CORREO = "juan@example.com";
    private static final String PASSWORD = "TempPass123!";
    private static final UUID ROL_ID = UUID.randomUUID();
    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final String KEYCLOAK_ID = "kc-uuid-123";

    private Usuario mockUsuario(UUID id, String keycloakId) {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(UsuarioId.from(id));
        when(usuario.getNombre()).thenReturn(NOMBRE);
        when(usuario.getCorreo()).thenReturn(CORREO);
        when(usuario.getRolId()).thenReturn(RolId.from(ROL_ID));
        when(usuario.getCreadoEn()).thenReturn(LocalDateTime.now());
        when(usuario.isActivo()).thenReturn(true);
        when(usuario.getKeycloakId()).thenReturn(keycloakId);
        return usuario;
    }

    // ════════════════════════════════════════════════════════════════
    // POST /api/usuarios/create
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/usuarios/create")
    class CreateEndpoint {

        @Test
        @DisplayName("success → 201 Created, response body contains usuario, no password field")
        void create_success_returns201WithJsonBody() throws Exception {
            Usuario usuario = mockUsuario(USUARIO_ID, KEYCLOAK_ID);
            when(createUseCase.create(any(CreateUsuarioCommand.class))).thenReturn(usuario);

            String body = """
                {
                  "nombre": "%s",
                  "correo": "%s",
                  "rolId": "%s",
                  "initialPassword": "%s",
                  "keycloakId": null
                }
                """.formatted(NOMBRE, CORREO, ROL_ID, PASSWORD);

            mockMvc.perform(post("/api/usuarios/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(USUARIO_ID.toString()))
                    .andExpect(jsonPath("$.nombre").value(NOMBRE))
                    .andExpect(jsonPath("$.correo").value(CORREO))
                    .andExpect(jsonPath("$.rolId").value(ROL_ID.toString()))
                    .andExpect(jsonPath("$.activo").value(true))
                    .andExpect(jsonPath("$.keycloakId").value(nullValue()))
                    .andExpect(jsonPath("$.initialPassword").doesNotExist())
                    .andExpect(jsonPath("$.password").doesNotExist());

            verify(createUseCase).create(any(CreateUsuarioCommand.class));
        }

        @Test
        @DisplayName("blank initialPassword → 400 Bad Request")
        void create_blankPassword_returns400() throws Exception {
            String body = """
                {
                  "nombre": "Juan Perez",
                  "correo": "juan@example.com",
                  "rolId": "%s",
                  "initialPassword": "",
                  "keycloakId": null
                }
                """.formatted(ROL_ID);

            mockMvc.perform(post("/api/usuarios/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("null initialPassword → 400 Bad Request")
        void create_nullPassword_returns400() throws Exception {
            String body = """
                {
                  "nombre": "Juan Perez",
                  "correo": "juan@example.com",
                  "rolId": "%s",
                  "initialPassword": null,
                  "keycloakId": null
                }
                """.formatted(ROL_ID);

            mockMvc.perform(post("/api/usuarios/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Keycloak CONNECTION_FAILURE → 502 Bad Gateway")
        void create_keycloakConnectionFailure_returns502() throws Exception {
            when(createUseCase.create(any(CreateUsuarioCommand.class)))
                    .thenThrow(new IdentityProvisioningException(
                            "Keycloak unreachable",
                            IdentityProvisioningException.Reason.CONNECTION_FAILURE
                    ));

            String body = """
                {
                  "nombre": "%s",
                  "correo": "%s",
                  "rolId": "%s",
                  "initialPassword": "%s",
                  "keycloakId": null
                }
                """.formatted(NOMBRE, CORREO, ROL_ID, PASSWORD);

            mockMvc.perform(post("/api/usuarios/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.error").value("Keycloak unreachable"));
        }

        @Test
        @DisplayName("Keycloak USER_ALREADY_EXISTS → 409 Conflict")
        void create_keycloakUserAlreadyExists_returns409() throws Exception {
            when(createUseCase.create(any(CreateUsuarioCommand.class)))
                    .thenThrow(new IdentityProvisioningException(
                            "kc-123",
                            "User already exists",
                            IdentityProvisioningException.Reason.USER_ALREADY_EXISTS
                    ));

            String body = """
                {
                  "nombre": "%s",
                  "correo": "%s",
                  "rolId": "%s",
                  "initialPassword": "%s",
                  "keycloakId": null
                }
                """.formatted(NOMBRE, CORREO, ROL_ID, PASSWORD);

            mockMvc.perform(post("/api/usuarios/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("User already exists"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // PUT /api/usuarios/edit
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/usuarios/edit")
    class EditEndpoint {

        @Test
        @DisplayName("success → 200 OK with JSON response body")
        void edit_success_returns200WithJson() throws Exception {
            Usuario usuario = mockUsuario(USUARIO_ID, KEYCLOAK_ID);
            when(editUseCase.edit(any(EditUsuarioCommand.class))).thenReturn(usuario);

            String body = """
                {
                  "nombre": "%s",
                  "correo": "%s",
                  "rolId": "%s",
                  "keycloakId": null
                }
                """.formatted(NOMBRE, CORREO, ROL_ID);

            mockMvc.perform(put("/api/usuarios/edit")
                            .param("id", USUARIO_ID.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(USUARIO_ID.toString()))
                    .andExpect(jsonPath("$.nombre").value(NOMBRE))
                    .andExpect(jsonPath("$.correo").value(CORREO))
                    .andExpect(jsonPath("$.keycloakId").value(nullValue()));

            verify(editUseCase).edit(any(EditUsuarioCommand.class));
        }

        @Test
        @DisplayName("success → correct fields passed to EditUsuarioCommand")
        void edit_passesCorrectFieldsToCommand() throws Exception {
            String nuevoNombre = "Juan Actualizado";
            String nuevoCorreo = "nuevo@example.com";
            UUID nuevoRolId = UUID.randomUUID();

            Usuario usuario = mockUsuario(USUARIO_ID, KEYCLOAK_ID);
            when(editUseCase.edit(any(EditUsuarioCommand.class))).thenReturn(usuario);

            String body = """
                {
                  "nombre": "%s",
                  "correo": "%s",
                  "rolId": "%s",
                  "keycloakId": null
                }
                """.formatted(nuevoNombre, nuevoCorreo, nuevoRolId);

            mockMvc.perform(put("/api/usuarios/edit")
                            .param("id", USUARIO_ID.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());

            verify(editUseCase).edit(argThat(cmd ->
                    cmd.id().equals(USUARIO_ID) &&
                            cmd.nombre().equals(nuevoNombre) &&
                            cmd.correo().equals(nuevoCorreo) &&
                            cmd.rolId().equals(nuevoRolId)
            ));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // DELETE /api/usuarios/delete
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/usuarios/delete")
    class DeleteEndpoint {

        @Test
        @DisplayName("success → 204 No Content")
        void delete_success_returns204() throws Exception {
            doNothing().when(deleteUseCase).delete(any(DeleteUsuarioCommand.class));

            mockMvc.perform(delete("/api/usuarios/delete")
                            .param("id", USUARIO_ID.toString()))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).delete(any(DeleteUsuarioCommand.class));
        }

        @Test
        @DisplayName("success → correct UUID passed to DeleteUsuarioCommand")
        void delete_passesCorrectIdToCommand() throws Exception {
            doNothing().when(deleteUseCase).delete(any(DeleteUsuarioCommand.class));

            mockMvc.perform(delete("/api/usuarios/delete")
                            .param("id", USUARIO_ID.toString()))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).delete(argThat(cmd -> cmd.id().equals(USUARIO_ID)));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // GET /api/usuarios/get-all (sanity check)
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/usuarios/get-all")
    class GetAllEndpoint {

        @Test
        @DisplayName("returns 200 OK with list of UsuarioResponse")
        void getAll_returns200WithList() throws Exception {
            Usuario u1 = mockUsuario(UUID.randomUUID(), "kc-1");
            Usuario u2 = mockUsuario(UUID.randomUUID(), "kc-2");
            when(getAllUseCase.getAll()).thenReturn(List.of(u1, u2));

            mockMvc.perform(get("/api/usuarios/get-all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].nombre").value(NOMBRE))
                    .andExpect(jsonPath("$[1].nombre").value(NOMBRE));
        }
    }
}
