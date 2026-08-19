package com.ar.crm2.adapter.in.rest;

import com.ar.crm2.adapter.in.rest.dto.request.AsignarColumnaRequest;
import com.ar.crm2.adapter.in.rest.dto.request.CreateTableroRequest;
import com.ar.crm2.adapter.in.rest.dto.request.EditTableroRequest;
import com.ar.crm2.adapter.in.rest.dto.request.ReordenarColumnasRequest;
import com.ar.crm2.adapter.in.rest.mapper.TableroResponseAssembler;
import com.ar.crm2.adapter.out.persistence.repository.ColumnaRepository;
import com.ar.crm2.application.security.ActorContext;
import com.ar.crm2.application.tablero.command.AsignarColumnaTableroCommand;
import com.ar.crm2.application.tablero.command.CreateTableroCommand;
import com.ar.crm2.application.tablero.command.DeleteTableroCommand;
import com.ar.crm2.application.tablero.command.EditTableroCommand;
import com.ar.crm2.application.tablero.command.EliminarColumnaDelTableroCommand;
import com.ar.crm2.application.tablero.command.GetTableroByIdCommand;
import com.ar.crm2.application.tablero.command.ReordenarColumnasCommand;
import com.ar.crm2.application.tablero.port.in.AsignarColumnaTableroUseCase;
import com.ar.crm2.application.tablero.port.in.CreateTableroUseCase;
import com.ar.crm2.application.tablero.port.in.DeleteTableroUseCase;
import com.ar.crm2.application.tablero.port.in.EditTableroUseCase;
import com.ar.crm2.application.tablero.port.in.EliminarColumnaDelTableroUseCase;
import com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase;
import com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase;
import com.ar.crm2.application.tablero.port.in.ReordenarColumnasUseCase;
import com.ar.crm2.model.entity.Tablero;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.TableroId;
import com.ar.crm2.security.KeycloakJwtActorContextMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TableroController REST adapter.
 *
 * @WebMvcTest loads only the web layer:
 * - Registers controllers, GlobalExceptionHandler and response mappers
 * - Configures MockMvc with real JSON serialization (Jackson)
 * - Does NOT load JPA, datasource or the full application context
 *
 * Use cases are mocked with @MockitoBean because they belong to the
 * application layer, not the web layer.
 *
 * {@link KeycloakJwtActorContextMapper} is mocked with @MockitoBean because
 * {@link com.ar.crm2.security.ActorContextRequestAttributeFilter} (a
 * {@code @Component}) constructor-injects it — without this override the MVC
 * slice fails to bootstrap with NoSuchBeanDefinitionException.
 *
 * {@code @WithMockUser} permits requests to reach the controller through
 * Spring Security. The {@code @BeforeEach} stub returns a non-null
 * {@link ActorContext} for any Authentication, so the request attribute
 * expected by {@code TableroController.create} is always populated.
 */
@WebMvcTest(controllers = {TableroController.class, GlobalExceptionHandler.class})
@Import(TableroResponseAssembler.class)
@WithMockUser
class TableroControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateTableroUseCase createUseCase;

    @MockitoBean
    private GetAllTablerosUseCase getAllUseCase;

    @MockitoBean
    private GetTableroByIdUseCase getByIdUseCase;

    @MockitoBean
    private EditTableroUseCase editUseCase;

    @MockitoBean
    private DeleteTableroUseCase deleteUseCase;

    @MockitoBean
    private AsignarColumnaTableroUseCase asignarColumnaUseCase;

    @MockitoBean
    private EliminarColumnaDelTableroUseCase eliminarColumnaUseCase;

    @MockitoBean
    private ReordenarColumnasUseCase reordenarColumnasUseCase;

    @MockitoBean
    private ColumnaRepository columnaRepository;

    @MockitoBean
    private KeycloakJwtActorContextMapper actorContextMapper;

    // ── Setup ───────────────────────────────────────────────────────

    @BeforeEach
    void stubActorContextMapper() {
        // ActorContextRequestAttributeFilter calls this on every authenticated
        // request. The mapper's real implementation throws IllegalArgumentException
        // for non-Jwt principals (which is what @WithMockUser provides), so the
        // stub is required to keep the request flowing.
        // TableroController.create derives the actor id from superUsuarioId() or
        // usuarioId() — we set superUsuarioId to a random UUID so the request
        // does not fail with AuthenticatedUsuarioRequiredException.
        ActorContext actorContext = new ActorContext(
                "test-subject",
                "test-user",
                "test@example.com",
                Optional.empty(),
                Optional.of(UUID.randomUUID()),
                Set.of("USER")
        );
        lenient().when(actorContextMapper.map(any(Authentication.class))).thenReturn(actorContext);
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private Tablero buildTablero(UUID id, String nombre) {
        return Tablero.reconstitute(
                TableroId.from(id),
                nombre,
                "Board description",
                List.of(),
                TipoTablero.TAREAS,
                LocalDateTime.now()
        );
    }

    // ── POST /api/tableros/create ─────────────────────────────────

    @Test
    void create_shouldReturn201WithTableroJson() throws Exception {
        UUID id = UUID.randomUUID();
        Tablero tablero = buildTablero(id, "Sprint Board");

        when(createUseCase.create(any(CreateTableroCommand.class))).thenReturn(tablero);

        String body = """
                {
                  "nombre": "Sprint Board",
                  "descripcion": "Board description",
                  "tipoTablero": "TAREAS",
                  "superUsuarioId": "%s",
                  "columnasPredeterminadas": true
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/tableros/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.nombre").value("Sprint Board"));

        verify(createUseCase).create(any(CreateTableroCommand.class));
    }

    // ── GET /api/tableros/get-all ────────────────────────────────

    @Test
    void getAll_shouldReturn200WithListOfTableros() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(getAllUseCase.getAll()).thenReturn(List.of(
                buildTablero(id1, "Board 1"),
                buildTablero(id2, "Board 2")
        ));

        mockMvc.perform(get("/api/tableros/get-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(id1.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Board 1"));
    }

    // ── GET /api/tableros/get-by-id ─────────────────────────────

    @Test
    void getById_shouldReturn200WithTablero() throws Exception {
        UUID id = UUID.randomUUID();

        when(getByIdUseCase.getById(any(GetTableroByIdCommand.class)))
                .thenReturn(buildTablero(id, "Sprint Board"));

        mockMvc.perform(get("/api/tableros/get-by-id")
                        .param("id", id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.nombre").value("Sprint Board"));
    }

    @Test
    void getById_shouldPassCorrectIdAsRequestParam() throws Exception {
        UUID id = UUID.randomUUID();

        when(getByIdUseCase.getById(any())).thenReturn(buildTablero(id, "Board"));

        mockMvc.perform(get("/api/tableros/get-by-id")
                        .param("id", id.toString()))
                .andExpect(status().isOk());

        verify(getByIdUseCase).getById(argThat(cmd -> id.equals(cmd.id())));
    }

    // ── PUT /api/tableros/edit ───────────────────────────────────

    @Test
    void edit_shouldReturn200WithUpdatedTablero() throws Exception {
        UUID id = UUID.randomUUID();
        Tablero tablero = buildTablero(id, "Updated Board");

        when(editUseCase.edit(any(EditTableroCommand.class))).thenReturn(tablero);

        String body = """
                {
                  "nombre": "Updated Board",
                  "descripcion": "New description"
                }
                """;

        mockMvc.perform(put("/api/tableros/edit")
                        .param("id", id.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.nombre").value("Updated Board"));
    }

    // ── DELETE /api/tableros/delete ─────────────────────────────

    @Test
    void delete_shouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();

        doNothing().when(deleteUseCase).delete(any(DeleteTableroCommand.class));

        mockMvc.perform(delete("/api/tableros/delete")
                        .param("id", id.toString()))
                .andExpect(status().isNoContent());

        verify(deleteUseCase).delete(any(DeleteTableroCommand.class));
    }

    @Test
    void agregarColumna_shouldReturn404BecauseEndpointWasRemoved() throws Exception {
        UUID tableroId = UUID.randomUUID();

        String body = """
                {
                  "nombre": "New Column",
                  "color": "#FF0000",
                  "tipoColumna": "PERSONALIZADA",
                  "limiteWip": 5,
                  "nota": "A note",
                  "totalValorEstimado": 1000,
                  "existeOtraColumnaConMismoNombre": false
                }
                """;

        mockMvc.perform(post("/api/tableros/agregar-columna")
                        .param("id", tableroId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/tableros/eliminar-columna ────────────────────

    @Test
    void eliminarColumna_shouldReturn204() throws Exception {
        UUID tableroId = UUID.randomUUID();
        UUID columnaId = UUID.randomUUID();

        when(eliminarColumnaUseCase.eliminarColumna(any(EliminarColumnaDelTableroCommand.class)))
                .thenReturn(buildTablero(tableroId, "Board"));

        mockMvc.perform(delete("/api/tableros/eliminar-columna")
                        .param("id", tableroId.toString())
                        .param("columnaId", columnaId.toString()))
                .andExpect(status().isNoContent());

        verify(eliminarColumnaUseCase)
                .eliminarColumna(any(EliminarColumnaDelTableroCommand.class));
    }

    // ── POST /api/tableros/asignar-columna ───────────────────────

    @Test
    void asignarColumna_shouldReturn201WithTableroJson() throws Exception {
        UUID tableroId = UUID.randomUUID();
        UUID columnaId = UUID.randomUUID();
        Tablero tablero = buildTablero(tableroId, "Board with assigned column");

        when(asignarColumnaUseCase.asignarColumna(any(AsignarColumnaTableroCommand.class)))
                .thenReturn(tablero);

        String body = """
                {
                  "limiteWip": 3,
                  "nota": "Assignment note",
                  "totalValorEstimado": 500
                }
                """;

        mockMvc.perform(post("/api/tableros/asignar-columna")
                        .param("id", tableroId.toString())
                        .param("columnaId", columnaId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(tableroId.toString()));

        verify(asignarColumnaUseCase).asignarColumna(any(AsignarColumnaTableroCommand.class));
    }

    @Test
    void asignarColumna_shouldPassCorrectIdsAndRequestBody() throws Exception {
        UUID tableroId = UUID.randomUUID();
        UUID columnaId = UUID.randomUUID();

        when(asignarColumnaUseCase.asignarColumna(any())).thenReturn(buildTablero(tableroId, "Board"));

        String body = """
                {
                  "limiteWip": 2,
                  "nota": "Test note",
                  "totalValorEstimado": 200
                }
                """;

        mockMvc.perform(post("/api/tableros/asignar-columna")
                        .param("id", tableroId.toString())
                        .param("columnaId", columnaId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(asignarColumnaUseCase).asignarColumna(argThat(cmd ->
                tableroId.equals(cmd.tableroId()) &&
                        columnaId.equals(cmd.columnaId()) &&
                        Integer.valueOf(2).equals(cmd.limiteWip()) &&
                        "Test note".equals(cmd.nota())
        ));
    }

    // ── PUT /api/tableros/reordenar-columnas ─────────────────────

    @Test
    void reordenarColumnas_shouldReturn200WithReorderedTablero() throws Exception {
        UUID tableroId = UUID.randomUUID();
        UUID col1 = UUID.randomUUID();
        UUID col2 = UUID.randomUUID();
        Tablero tablero = buildTablero(tableroId, "Reordered Board");

        when(reordenarColumnasUseCase.reordenar(any(ReordenarColumnasCommand.class)))
                .thenReturn(tablero);

        String body = """
                {
                  "nuevoOrden": [
                    "%s",
                    "%s"
                  ]
                }
                """.formatted(col1, col2);

        mockMvc.perform(put("/api/tableros/reordenar-columnas")
                        .param("id", tableroId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tableroId.toString()));

        verify(reordenarColumnasUseCase).reordenar(any(ReordenarColumnasCommand.class));
    }
}
