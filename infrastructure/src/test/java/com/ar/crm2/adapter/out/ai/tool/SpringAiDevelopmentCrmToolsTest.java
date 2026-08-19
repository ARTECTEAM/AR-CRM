package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.TablerosOutput;
import com.ar.crm2.application.columna.command.CreateColumnaCommand;
import com.ar.crm2.application.columna.port.in.CreateColumnaUseCase;
import com.ar.crm2.application.columna.port.in.EditColumnaUseCase;
import com.ar.crm2.application.columna.port.in.GetAllColumnasUseCase;
import com.ar.crm2.application.columna.port.in.GetColumnaByIdUseCase;
import com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase;
import com.ar.crm2.application.ficha.port.in.GetFichaByIdUseCase;
import com.ar.crm2.application.tablero.command.CreateTableroCommand;
import com.ar.crm2.application.tablero.port.in.AsignarColumnaTableroUseCase;
import com.ar.crm2.application.tablero.port.in.CreateTableroUseCase;
import com.ar.crm2.application.tablero.port.in.EditTableroUseCase;
import com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase;
import com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase;
import com.ar.crm2.application.tablero.port.in.ReordenarColumnasUseCase;
import com.ar.crm2.model.enums.TipoColumna;
import com.ar.crm2.model.enums.TipoTablero;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiDevelopmentCrmToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void exposesExactlyTwelveReadAndBoardColumnMutationCallbacks() {
        Map<String, ToolCallback> callbacks = Arrays.stream(ToolCallbacks.from(fixture(null, null, null)))
                .collect(Collectors.toMap(callback -> callback.getToolDefinition().name(), callback -> callback));

        assertThat(callbacks.keySet()).containsExactlyInAnyOrder(
                "list_tableros", "get_tablero", "create_tablero", "edit_tablero",
                "assign_columna_to_tablero", "reorder_tablero_columns",
                "list_columnas", "get_columna", "create_columna", "edit_columna",
                "list_fichas", "get_ficha");
        assertThat(callbacks.keySet()).noneMatch(name -> name.contains("delete") || name.contains("remove"));
        for (ToolCallback callback : callbacks.values()) {
            assertThat(callback.getToolDefinition().inputSchema())
                    .doesNotContain("actorUsuarioId", "actorSuperUsuarioId", "turnId", "agentOwnerId", "ToolContext");
        }
    }

    @Test
    void readMethodReturnsTypedOutputAndCallbackSerializesStructuredJson() throws Exception {
        GetAllTablerosUseCase tableros = mock(GetAllTablerosUseCase.class);
        when(tableros.getAll()).thenReturn(List.of());
        SpringAiDevelopmentCrmTools tools = fixture(null, null, tableros);
        ToolContext context = trustedContext(UUID.randomUUID(), null);

        TablerosOutput typed = tools.listTableros(context);
        assertThat(typed.tableros()).isEmpty();
        ToolCallback callback = Arrays.stream(ToolCallbacks.from(tools))
                .filter(candidate -> candidate.getToolDefinition().name().equals("list_tableros"))
                .findFirst().orElseThrow();
        JsonNode serialized = MAPPER.readTree(callback.call("{}", context));
        assertThat(serialized.get("total").asInt()).isZero();
        assertThat(serialized.get("truncated").asBoolean()).isFalse();
    }

    @Test
    void everyCallbackRejectsAbsentTrustedActorBeforeDelegation() {
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
                Map.entry("get_ficha", "{\"id\":\"00000000-0000-0000-0000-000000000001\"}"));
        for (ToolCallback callback : ToolCallbacks.from(fixture(null, null, null))) {
            Throwable failure = catchThrowable(() -> callback.call(
                    arguments.get(callback.getToolDefinition().name()),
                    new ToolContext(Map.of("unrelatedTrustedContext", "present"))));
            assertThat(failure).isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
            assertThat(failure.getCause()).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void representativeBoardAndColumnCommandsUseTrustedArguments() {
        CreateTableroUseCase createTablero = mock(CreateTableroUseCase.class);
        CreateColumnaUseCase createColumna = mock(CreateColumnaUseCase.class);
        SpringAiDevelopmentCrmTools tools = fixture(createTablero, createColumna, null);
        UUID actor = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID superUsuario = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        ToolContext context = trustedContext(actor, superUsuario);

        tools.createTablero("Board", "Description", "TAREAS", context);
        tools.createColumna("Default", "#112233", "TAREAS", "PREDETERMINADA", context);

        ArgumentCaptor<CreateTableroCommand> tablero = ArgumentCaptor.forClass(CreateTableroCommand.class);
        verify(createTablero).create(tablero.capture());
        assertThat(tablero.getValue().actorId()).isEqualTo(actor);
        assertThat(tablero.getValue().tipoTablero()).isEqualTo(TipoTablero.TAREAS);
        ArgumentCaptor<CreateColumnaCommand> columna = ArgumentCaptor.forClass(CreateColumnaCommand.class);
        verify(createColumna).create(columna.capture());
        assertThat(columna.getValue().superUsuarioId()).contains(superUsuario);
        assertThat(columna.getValue().tipoColumna()).isEqualTo(TipoColumna.PREDETERMINADA);
    }

    @Test
    void normalActorCanCreatePersonalizadaButCannotCreatePredeterminada() {
        CreateColumnaUseCase createColumna = mock(CreateColumnaUseCase.class);
        SpringAiDevelopmentCrmTools tools = fixture(null, createColumna, null);
        ToolContext normalActor = trustedContext(UUID.randomUUID(), null);
        tools.createColumna("Custom", null, "TRATOS", "PERSONALIZADA", normalActor);
        Throwable failure = catchThrowable(() ->
                tools.createColumna("Default", null, "TRATOS", "PREDETERMINADA", normalActor));
        assertThat(failure).isInstanceOf(SafeToolValidationException.class);
        verify(createColumna, org.mockito.Mockito.times(1)).create(any());
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
            GetAllTablerosUseCase tableros) {
        return new SpringAiDevelopmentCrmTools(
                tableros == null ? mock(GetAllTablerosUseCase.class) : tableros,
                mock(GetTableroByIdUseCase.class),
                createTablero == null ? mock(CreateTableroUseCase.class) : createTablero,
                mock(EditTableroUseCase.class),
                mock(AsignarColumnaTableroUseCase.class),
                mock(ReordenarColumnasUseCase.class),
                mock(GetAllColumnasUseCase.class),
                mock(GetColumnaByIdUseCase.class),
                createColumna == null ? mock(CreateColumnaUseCase.class) : createColumna,
                mock(EditColumnaUseCase.class),
                mock(GetAllFichasUseCase.class),
                mock(GetFichaByIdUseCase.class));
    }
}
