package com.ar.crm2.adapter.in.rest;

import com.ar.crm2.adapter.in.rest.dto.request.CreateEtiquetaRequest;
import com.ar.crm2.adapter.in.rest.dto.request.EditEtiquetaRequest;
import com.ar.crm2.adapter.in.rest.mapper.EtiquetaCommandMapper;
import com.ar.crm2.application.etiqueta.command.CreateEtiquetaCommand;
import com.ar.crm2.application.etiqueta.command.DeleteEtiquetaCommand;
import com.ar.crm2.application.etiqueta.command.EditEtiquetaCommand;
import com.ar.crm2.application.etiqueta.command.GetAllEtiquetasCommand;
import com.ar.crm2.application.etiqueta.command.GetEtiquetaByIdCommand;
import com.ar.crm2.application.etiqueta.exception.EtiquetaNotFoundException;
import com.ar.crm2.application.etiqueta.port.in.CreateEtiquetaUseCase;
import com.ar.crm2.application.etiqueta.port.in.DeleteEtiquetaUseCase;
import com.ar.crm2.application.etiqueta.port.in.EditEtiquetaUseCase;
import com.ar.crm2.application.etiqueta.port.in.GetAllEtiquetasUseCase;
import com.ar.crm2.application.etiqueta.port.in.GetEtiquetaByIdUseCase;
import com.ar.crm2.model.entity.Etiqueta;
import com.ar.crm2.model.enums.TipoEtiqueta;
import com.ar.crm2.model.vo.EtiquetaId;
import com.ar.crm2.security.KeycloakJwtActorContextMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for EtiquetaController REST adapter.
 *
 * <p>{@code @WebMvcTest} loads the web layer with real JSON serialization
 * (Jackson). Use cases are mocked with {@code @MockitoBean} because they
 * belong to the application layer. {@link KeycloakJwtActorContextMapper} is
 * mocked to keep the security filter from blowing up under {@code @WithMockUser}.
 */
@WebMvcTest(controllers = {EtiquetaController.class, GlobalExceptionHandler.class})
@WithMockUser
class EtiquetaControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateEtiquetaUseCase createUseCase;

    @MockitoBean
    private GetAllEtiquetasUseCase getAllUseCase;

    @MockitoBean
    private GetEtiquetaByIdUseCase getByIdUseCase;

    @MockitoBean
    private EditEtiquetaUseCase editUseCase;

    @MockitoBean
    private DeleteEtiquetaUseCase deleteUseCase;

    @MockitoBean
    private KeycloakJwtActorContextMapper actorContextMapper;

    // Response advice is component-scanned even in this controller slice.
    @MockitoBean
    private com.ar.crm2.application.security.port.out.CurrentActorPort currentActorPort;

    @MockitoBean
    private com.ar.crm2.application.security.CrmAuthorization crmAuthorization;

    // ── Helpers ─────────────────────────────────────────────────────

    private Etiqueta buildEtiqueta(UUID id, String nombre, TipoEtiqueta tipo, String color) {
        return Etiqueta.reconstitute(
            EtiquetaId.from(id),
            nombre,
            tipo,
            color,
            LocalDateTime.now()
        );
    }

    // ── POST /api/etiquetas/create ──────────────────────────────────

    @Test
    void create_shouldReturn201WithEtiquetaJson() throws Exception {
        UUID id = UUID.randomUUID();
        when(createUseCase.create(any(CreateEtiquetaCommand.class)))
            .thenReturn(buildEtiqueta(id, "Urgent", TipoEtiqueta.TAREA, "#FF0000"));

        String body = """
                {
                  "nombre": "Urgent",
                  "tipoEtiqueta": "TAREA",
                  "color": "#FF0000"
                }
                """;

        mockMvc.perform(post("/api/etiquetas/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.nombre").value("Urgent"))
                .andExpect(jsonPath("$.tipoEtiqueta").value("TAREA"))
                .andExpect(jsonPath("$.color").value("#FF0000"));

        verify(createUseCase).create(any(CreateEtiquetaCommand.class));
    }

    @Test
    void create_shouldMapRequestBodyFieldsToCommand() throws Exception {
        UUID id = UUID.randomUUID();
        when(createUseCase.create(any())).thenReturn(buildEtiqueta(id, "VIP", TipoEtiqueta.TRATO, "#FFD700"));

        String body = """
                {
                  "nombre": "VIP",
                  "tipoEtiqueta": "TRATO",
                  "color": "#FFD700"
                }
                """;

        mockMvc.perform(post("/api/etiquetas/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(createUseCase).create(argThat(cmd ->
            "VIP".equals(cmd.nombre()) &&
            cmd.tipoEtiqueta() == TipoEtiqueta.TRATO &&
            "#FFD700".equals(cmd.color())
        ));
    }

    // ── GET /api/etiquetas/get-all ──────────────────────────────────

    @Test
    void getAll_shouldReturn200WithListOfEtiquetas() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(getAllUseCase.getAll(any())).thenReturn(List.of(
            buildEtiqueta(id1, "A", TipoEtiqueta.TAREA, "#000000"),
            buildEtiqueta(id2, "B", TipoEtiqueta.TRATO, "#FFFFFF")
        ));

        mockMvc.perform(get("/api/etiquetas/get-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(id1.toString()))
                .andExpect(jsonPath("$[0].nombre").value("A"))
                .andExpect(jsonPath("$[1].id").value(id2.toString()))
                .andExpect(jsonPath("$[1].nombre").value("B"));
    }

    @Test
    void getAll_withTipoFilter_shouldPassFilterToUseCase() throws Exception {
        UUID id1 = UUID.randomUUID();
        when(getAllUseCase.getAll(any()))
            .thenReturn(List.of(buildEtiqueta(id1, "A", TipoEtiqueta.TAREA, "#000000")));

        mockMvc.perform(get("/api/etiquetas/get-all")
                        .param("tipoEtiqueta", "TAREA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(getAllUseCase).getAll(argThat(opt ->
            opt.isPresent() && opt.get() == TipoEtiqueta.TAREA
        ));
    }

    // ── GET /api/etiquetas/get-by-id ────────────────────────────────

    @Test
    void getById_shouldReturn200WithEtiqueta() throws Exception {
        UUID id = UUID.randomUUID();
        when(getByIdUseCase.getById(any(GetEtiquetaByIdCommand.class)))
            .thenReturn(buildEtiqueta(id, "Urgent", TipoEtiqueta.TAREA, "#FF0000"));

        mockMvc.perform(get("/api/etiquetas/get-by-id")
                        .param("id", id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.nombre").value("Urgent"));
    }

    @Test
    void getById_shouldReturn404WhenEtiquetaDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(getByIdUseCase.getById(any(GetEtiquetaByIdCommand.class)))
            .thenThrow(EtiquetaNotFoundException.forId(id));

        mockMvc.perform(get("/api/etiquetas/get-by-id")
                        .param("id", id.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString(id.toString())));
    }

    // ── PUT /api/etiquetas/edit ─────────────────────────────────────

    @Test
    void edit_shouldReturn200WithUpdatedEtiqueta() throws Exception {
        UUID id = UUID.randomUUID();
        when(editUseCase.edit(any(EditEtiquetaCommand.class)))
            .thenReturn(buildEtiqueta(id, "Renamed", TipoEtiqueta.TAREA, "#00FF00"));

        String body = """
                {
                  "nombre": "Renamed",
                  "color": "#00FF00"
                }
                """;

        mockMvc.perform(put("/api/etiquetas/edit")
                        .param("id", id.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.nombre").value("Renamed"))
                .andExpect(jsonPath("$.color").value("#00FF00"));
    }

    // ── DELETE /api/etiquetas/delete ────────────────────────────────

    @Test
    void delete_shouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(deleteUseCase).delete(any(DeleteEtiquetaCommand.class));

        mockMvc.perform(delete("/api/etiquetas/delete")
                        .param("id", id.toString())
                        .param("confirm", "true"))
                .andExpect(status().isNoContent());

        verify(deleteUseCase).delete(argThat(cmd ->
            id.equals(cmd.id()) && cmd.confirm()
        ));
    }

    @Test
    void delete_withoutConfirm_shouldStillBeReachable() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(deleteUseCase).delete(any(DeleteEtiquetaCommand.class));

        mockMvc.perform(delete("/api/etiquetas/delete")
                        .param("id", id.toString())
                        .param("confirm", "false"))
                .andExpect(status().isNoContent());

        verify(deleteUseCase).delete(argThat(cmd ->
            id.equals(cmd.id()) && !cmd.confirm()
        ));
    }
}
