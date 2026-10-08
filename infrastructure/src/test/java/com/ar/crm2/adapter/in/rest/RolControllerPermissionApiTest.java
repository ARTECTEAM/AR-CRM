package com.ar.crm2.adapter.in.rest;

import com.ar.crm2.adapter.in.rest.dto.request.CreateRolRequest;
import com.ar.crm2.application.rol.command.CreateRolCommand;
import com.ar.crm2.application.rol.command.EditRolCommand;
import com.ar.crm2.application.rol.port.in.CreateRolUseCase;
import com.ar.crm2.application.rol.port.in.DeleteRolUseCase;
import com.ar.crm2.application.rol.port.in.EditRolUseCase;
import com.ar.crm2.application.rol.port.in.GetAllRolesUseCase;
import com.ar.crm2.application.rol.port.in.GetRolByIdUseCase;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.PermisoRecurso;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Rol;
import com.ar.crm2.model.vo.RolId;
import com.ar.crm2.security.KeycloakJwtActorContextMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {RolController.class, GlobalExceptionHandler.class})
@WithMockUser
class RolControllerPermissionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private CreateRolUseCase createUseCase;
    @MockitoBean private GetAllRolesUseCase getAllUseCase;
    @MockitoBean private GetRolByIdUseCase getByIdUseCase;
    @MockitoBean private EditRolUseCase editUseCase;
    @MockitoBean private DeleteRolUseCase deleteUseCase;
    @MockitoBean private KeycloakJwtActorContextMapper actorContextMapper;
    @MockitoBean private CurrentActorPort currentActorPort;
    @MockitoBean private CrmAuthorization crmAuthorization;

    @Test
    void createAndReadRoleExposeConfiguredPermissionCatalog() throws Exception {
        UUID roleId = UUID.randomUUID();
        PermisoRecurso financeRead = financeReadGrant();
        Rol role = Rol.create("Sales reader", "Read-only finance", List.of(financeRead));
        when(createUseCase.create(any(CreateRolCommand.class))).thenReturn(role);
        when(getAllUseCase.getAll()).thenReturn(List.of(role));

        mockMvc.perform(post("/api/roles/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Sales reader",
                                  "descripcion": "Read-only finance",
                                  "permisos": [{
                                    "recurso": "TRATO",
                                    "acciones": ["LEER"],
                                    "alcance": "TODO_COMPARTIDO",
                                    "idsPermitidos": [],
                                    "gruposLectura": ["FINANCIERO"],
                                    "gruposEscritura": []
                                  }]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permisos[0].recurso").value("TRATO"))
                .andExpect(jsonPath("$.permisos[0].gruposLectura[0]").value("FINANCIERO"));

        var createCaptor = org.mockito.ArgumentCaptor.forClass(CreateRolCommand.class);
        verify(createUseCase).create(createCaptor.capture());
        assertThat(createCaptor.getValue().permisos()).containsExactly(financeRead);

        mockMvc.perform(get("/api/roles/get-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].permisos[0].recurso").value("TRATO"))
                .andExpect(jsonPath("$[0].permisos[0].acciones[0]").value("LEER"));
    }

    @Test
    void editCanRevokePermissionsAndDeactivateRoleThroughHttp() throws Exception {
        UUID roleId = UUID.randomUUID();
        when(editUseCase.edit(any(EditRolCommand.class))).thenReturn(
                Rol.reconstitute(RolId.from(roleId), "Sales reader", "Read-only finance", false, List.of()));

        mockMvc.perform(put("/api/roles/edit").param("id", roleId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Sales reader",
                                  "descripcion": "Read-only finance",
                                  "activo": false,
                                  "permisos": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.permisos").isArray())
                .andExpect(jsonPath("$.permisos").isEmpty());

        var editCaptor = org.mockito.ArgumentCaptor.forClass(EditRolCommand.class);
        verify(editUseCase).edit(editCaptor.capture());
        assertThat(editCaptor.getValue().activo()).isFalse();
        assertThat(editCaptor.getValue().permisos()).isEmpty();
    }

    @Test
    void permissionEscalationDenialIsReturnedAsForbiddenByActualRoleHttpEndpoint() throws Exception {
        UUID roleId = UUID.randomUUID();
        var captured = new java.util.concurrent.atomic.AtomicReference<EditRolCommand>();
        doAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            throw new CrmAuthorizationDeniedException("Cannot delegate permissions beyond the current grant");
        }).when(editUseCase).edit(any(EditRolCommand.class));

        mockMvc.perform(put("/api/roles/edit").param("id", roleId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Sales reader",
                                  "activo": true,
                                  "permisos": [{
                                    "recurso": "TRATO",
                                    "acciones": ["ADMINISTRAR"],
                                    "alcance": "TODO_COMPARTIDO",
                                    "idsPermitidos": [],
                                    "gruposLectura": ["FINANCIERO"],
                                    "gruposEscritura": ["FINANCIERO"]
                                  }]
                                }
                                """))
                .andExpect(status().isForbidden());

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().permisos()).containsExactly(new PermisoRecurso(RecursoCrm.TRATO,
                Set.of(AccionCrm.ADMINISTRAR), AlcanceCrm.TODO_COMPARTIDO, Set.of(),
                Set.of(GrupoCampoSensible.FINANCIERO), Set.of(GrupoCampoSensible.FINANCIERO)));
    }

    @Test
    void legacyRoleRequestsDefaultCreateGrantsAndPreserveOmittedEditState() throws Exception {
        UUID roleId = UUID.randomUUID();
        PermisoRecurso existingGrant = financeReadGrant();
        Rol existingRole = Rol.reconstitute(RolId.from(roleId), "Sales reader", "Existing description",
                true, List.of(existingGrant));
        when(createUseCase.create(any(CreateRolCommand.class))).thenReturn(Rol.create("Legacy", "No grants"));
        when(editUseCase.edit(any(EditRolCommand.class))).thenReturn(existingRole);

        mockMvc.perform(post("/api/roles/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Legacy","descripcion":"No grants"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permisos").isArray())
                .andExpect(jsonPath("$.permisos").isEmpty());

        var createCaptor = org.mockito.ArgumentCaptor.forClass(CreateRolCommand.class);
        verify(createUseCase).create(createCaptor.capture());
        assertThat(createCaptor.getValue().permisos()).isEmpty();

        mockMvc.perform(put("/api/roles/edit").param("id", roleId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Sales reader","descripcion":"Existing description"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.permisos[0].recurso").value("TRATO"));

        var editCaptor = org.mockito.ArgumentCaptor.forClass(EditRolCommand.class);
        verify(editUseCase).edit(editCaptor.capture());
        assertThat(editCaptor.getValue().activo()).isNull();
        assertThat(editCaptor.getValue().permisos()).isNull();
    }
    private static PermisoRecurso financeReadGrant() {
        return new PermisoRecurso(RecursoCrm.TRATO, Set.of(AccionCrm.LEER), AlcanceCrm.TODO_COMPARTIDO,
                Set.of(), Set.of(GrupoCampoSensible.FINANCIERO), Set.of());
    }
}