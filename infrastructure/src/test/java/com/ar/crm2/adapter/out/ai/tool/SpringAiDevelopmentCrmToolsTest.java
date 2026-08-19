package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.TablerosOutput;
import com.ar.crm2.application.columna.command.CreateColumnaCommand;
import com.ar.crm2.application.columna.port.in.CreateColumnaUseCase;
import com.ar.crm2.application.ficha.command.EditFichaCommand;
import com.ar.crm2.application.ficha.port.in.EditFichaUseCase;
import com.ar.crm2.application.tablero.command.CreateTableroCommand;
import com.ar.crm2.application.tablero.port.in.CreateTableroUseCase;
import com.ar.crm2.model.enums.TipoColumna;
import com.ar.crm2.model.enums.TipoFicha;
import com.ar.crm2.model.enums.TipoTablero;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiDevelopmentCrmToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void callbacksExposeExactSchemasWithoutTrustedIdentityOrDeleteOperations() throws Exception {
        SpringAiDevelopmentCrmTools tools = fixture(null, null, null);
        Map<String, ToolCallback> callbacks = Arrays.stream(ToolCallbacks.from(tools))
                .collect(Collectors.toMap(callback -> callback.getToolDefinition().name(), callback -> callback));

        assertThat(callbacks.keySet()).hasSize(15).noneMatch(name -> name.contains("delete") || name.contains("remove"));
        for (ToolCallback callback : callbacks.values()) {
            assertThat(callback.getToolDefinition().inputSchema())
                    .doesNotContain("actorUsuarioId", "actorSuperUsuarioId", "turnId", "agentOwnerId", "ToolContext");
        }
        JsonNode editFicha = MAPPER.readTree(callbacks.get("edit_ficha").getToolDefinition().inputSchema());
        Set<String> required = MAPPER.convertValue(editFicha.get("required"),
                MAPPER.getTypeFactory().constructCollectionType(Set.class, String.class));
        assertThat(required).contains("id", "columnaId", "tipoFicha", "etiquetaIds");
        assertThat(editFicha.get("properties").has("delete")).isFalse();
    }

    @Test
    void directMethodReturnsTypedOutputAndCallbackSerializesItAsStructuredJson() throws Exception {
        var listTableros = mock(com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase.class);
        when(listTableros.getAll()).thenReturn(List.of());
        SpringAiDevelopmentCrmTools tools = fixture(null, null, null, listTableros);
        ToolContext context = trustedContext(UUID.randomUUID(), null);

        TablerosOutput typed = tools.listTableros(context);

        assertThat(typed.tableros()).isEmpty();
        assertThat(typed.total()).isZero();
        assertThat(typed.truncated()).isFalse();

        ToolCallback callback = Arrays.stream(ToolCallbacks.from(tools))
                .filter(candidate -> candidate.getToolDefinition().name().equals("list_tableros"))
                .findFirst()
                .orElseThrow();
        JsonNode serialized = MAPPER.readTree(callback.call("{}", context));
        assertThat(serialized.get("tableros").isArray()).isTrue();
        assertThat(serialized.get("tableros")).isEmpty();
        assertThat(serialized.get("total").asInt()).isZero();
        assertThat(serialized.get("truncated").asBoolean()).isFalse();
    }

    @Test
    void everyDevelopmentCallbackRejectsAbsentTrustedActorBeforeDelegation() {
        SpringAiDevelopmentCrmTools tools = fixture(null, null, null);
        Map<String, String> arguments = Map.ofEntries(
                Map.entry("list_tableros", "{}"),
                Map.entry("get_tablero", "{\"id\":\"00000000-0000-0000-0000-000000000001\"}"),
                Map.entry("create_tablero", "{\"nombre\":\"B\",\"descripcion\":\"D\",\"tipoTablero\":\"TAREAS\"}"),
                Map.entry("edit_tablero", "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"nombre\":\"B\",\"descripcion\":\"D\"}"),
                Map.entry("assign_columna_to_tablero", "{\"tableroId\":\"00000000-0000-0000-0000-000000000001\",\"columnaId\":\"00000000-0000-0000-0000-000000000002\",\"limiteWip\":1,\"totalValorEstimado\":0}"),
                Map.entry("reorder_tablero_columns", "{\"tableroId\":\"00000000-0000-0000-0000-000000000001\",\"nuevoOrden\":[]}"),
                Map.entry("list_columnas", "{}"),
                Map.entry("get_columna", "{\"id\":\"00000000-0000-0000-0000-000000000001\"}"),
                Map.entry("create_columna", "{\"nombre\":\"C\",\"tipoTablero\":\"TAREAS\",\"tipoColumna\":\"PERSONALIZADA\"}"),
                Map.entry("edit_columna", "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"nombre\":\"C\",\"tipoTablero\":\"TAREAS\",\"tipoColumna\":\"PERSONALIZADA\"}"),
                Map.entry("list_fichas", "{}"),
                Map.entry("get_ficha", "{\"id\":\"00000000-0000-0000-0000-000000000001\"}"),
                Map.entry("create_ficha", "{\"columnaId\":\"00000000-0000-0000-0000-000000000001\",\"tipoFicha\":\"TRATO\",\"tratoId\":\"00000000-0000-0000-0000-000000000002\"}"),
                Map.entry("edit_ficha", "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"columnaId\":\"00000000-0000-0000-0000-000000000002\",\"tipoFicha\":\"TRATO\",\"tratoId\":\"00000000-0000-0000-0000-000000000003\",\"etiquetaIds\":[]}"),
                Map.entry("move_ficha_to_columna", "{\"fichaId\":\"00000000-0000-0000-0000-000000000001\",\"targetColumnaId\":\"00000000-0000-0000-0000-000000000002\"}"));

        for (ToolCallback callback : ToolCallbacks.from(tools)) {
            Throwable failure = catchThrowable(() -> callback.call(
                    arguments.get(callback.getToolDefinition().name()),
                    new ToolContext(Map.of("unrelatedTrustedContext", "present"))));
            assertThat(failure).as(callback.getToolDefinition().name())
                    .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
            assertThat(failure.getCause()).as(callback.getToolDefinition().name())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void representativeCommandsUseExactArgumentsForEachAggregate() throws Exception {
        CreateTableroUseCase createTablero = mock(CreateTableroUseCase.class);
        CreateColumnaUseCase createColumna = mock(CreateColumnaUseCase.class);
        EditFichaUseCase editFicha = mock(EditFichaUseCase.class);
        SpringAiDevelopmentCrmTools tools = fixture(createTablero, createColumna, editFicha);
        UUID actor = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID superUsuario = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID fichaId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID columnaId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        UUID tratoId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        UUID etiquetaId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        ToolContext context = trustedContext(actor, superUsuario);

        tools.createTablero("Board", "Description", "TAREAS", context);
        tools.createColumna("Default", "#112233", "TAREAS", "PREDETERMINADA", context);
        tools.editFicha(fichaId, columnaId, "TRATO", tratoId, null, List.of(etiquetaId), context);

        ArgumentCaptor<CreateTableroCommand> tablero = ArgumentCaptor.forClass(CreateTableroCommand.class);
        verify(createTablero).create(tablero.capture());
        assertThat(tablero.getValue().nombre()).isEqualTo("Board");
        assertThat(tablero.getValue().descripcion()).isEqualTo("Description");
        assertThat(tablero.getValue().tipoTablero()).isEqualTo(TipoTablero.TAREAS);
        assertThat(tablero.getValue().actorId()).isEqualTo(actor);

        ArgumentCaptor<CreateColumnaCommand> columna = ArgumentCaptor.forClass(CreateColumnaCommand.class);
        verify(createColumna).create(columna.capture());
        assertThat(columna.getValue().superUsuarioId()).contains(superUsuario);
        assertThat(columna.getValue().tipoColumna()).isEqualTo(TipoColumna.PREDETERMINADA);

        ArgumentCaptor<EditFichaCommand> ficha = ArgumentCaptor.forClass(EditFichaCommand.class);
        verify(editFicha).edit(ficha.capture());
        assertThat(ficha.getValue().id()).isEqualTo(fichaId);
        assertThat(ficha.getValue().columnaId()).isEqualTo(columnaId);
        assertThat(ficha.getValue().tipoFicha()).isEqualTo(TipoFicha.TRATO);
        assertThat(ficha.getValue().tratoId()).isEqualTo(tratoId);
        assertThat(ficha.getValue().etiquetaIds()).containsExactly(etiquetaId);
    }

    @Test
    void normalActorCanCreatePersonalizadaButCannotCreatePredeterminada() throws Exception {
        CreateColumnaUseCase createColumna = mock(CreateColumnaUseCase.class);
        SpringAiDevelopmentCrmTools tools = fixture(null, createColumna, null);
        ToolContext normalActor = trustedContext(UUID.randomUUID(), null);

        tools.createColumna("Custom", null, "TRATOS", "PERSONALIZADA", normalActor);
        ArgumentCaptor<CreateColumnaCommand> command = ArgumentCaptor.forClass(CreateColumnaCommand.class);
        verify(createColumna).create(command.capture());
        assertThat(command.getValue().superUsuarioId()).isEmpty();

        Throwable failure = catchThrowable(() ->
                tools.createColumna("Default", null, "TRATOS", "PREDETERMINADA", normalActor));
        assertThat(failure).isInstanceOf(SafeToolValidationException.class)
                .hasMessage("create_columna PREDETERMINADA requires a trusted super-user claim");
        verify(createColumna, org.mockito.Mockito.times(1)).create(any());
    }

    @Test
    void editFichaRejectsOmittedLabelsAndExplicitEmptyMeansClearAll() throws Exception {
        EditFichaUseCase editFicha = mock(EditFichaUseCase.class);
        SpringAiDevelopmentCrmTools tools = fixture(null, null, editFicha);
        ToolContext context = trustedContext(UUID.randomUUID(), null);
        UUID id = UUID.randomUUID();
        UUID column = UUID.randomUUID();
        UUID deal = UUID.randomUUID();

        Throwable omitted = catchThrowable(() ->
                tools.editFicha(id, column, "TRATO", deal, null, null, context));
        assertThat(omitted).isInstanceOf(SafeToolValidationException.class)
                .hasMessageContaining("requires etiquetaIds");
        verify(editFicha, never()).edit(any());

        tools.editFicha(id, column, "TRATO", deal, null, List.of(), context);
        ArgumentCaptor<EditFichaCommand> command = ArgumentCaptor.forClass(EditFichaCommand.class);
        verify(editFicha).edit(command.capture());
        assertThat(command.getValue().etiquetaIds()).isEmpty();
    }

    private static ToolContext trustedContext(UUID actor, UUID superUsuario) {
        Map<String, Object> context = new HashMap<>();
        context.put("actorUsuarioId", actor);
        if (superUsuario != null) context.put("actorSuperUsuarioId", superUsuario);
        return new ToolContext(Map.copyOf(context));
    }

    private static SpringAiDevelopmentCrmTools fixture(
            CreateTableroUseCase createTablero,
            CreateColumnaUseCase createColumna,
            EditFichaUseCase editFicha) {
        return fixture(createTablero, createColumna, editFicha,
                mock(com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase.class));
    }

    private static SpringAiDevelopmentCrmTools fixture(
            CreateTableroUseCase createTablero,
            CreateColumnaUseCase createColumna,
            EditFichaUseCase editFicha,
            com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase getAllTableros) {
        return new SpringAiDevelopmentCrmTools(
                getAllTableros,
                mock(com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase.class),
                createTablero == null ? mock(CreateTableroUseCase.class) : createTablero,
                mock(com.ar.crm2.application.tablero.port.in.EditTableroUseCase.class),
                mock(com.ar.crm2.application.tablero.port.in.AsignarColumnaTableroUseCase.class),
                mock(com.ar.crm2.application.tablero.port.in.ReordenarColumnasUseCase.class),
                mock(com.ar.crm2.application.columna.port.in.GetAllColumnasUseCase.class),
                mock(com.ar.crm2.application.columna.port.in.GetColumnaByIdUseCase.class),
                createColumna == null ? mock(CreateColumnaUseCase.class) : createColumna,
                mock(com.ar.crm2.application.columna.port.in.EditColumnaUseCase.class),
                mock(com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase.class),
                mock(com.ar.crm2.application.ficha.port.in.GetFichaByIdUseCase.class),
                mock(com.ar.crm2.application.ficha.port.in.CreateFichaUseCase.class),
                editFicha == null ? mock(EditFichaUseCase.class) : editFicha,
                mock(com.ar.crm2.application.ficha.port.in.MoverColumnaFichaUseCase.class));
    }
}
