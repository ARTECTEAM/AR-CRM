package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.TablerosOutput;
import com.ar.crm2.application.columna.port.in.GetAllColumnasUseCase;
import com.ar.crm2.application.columna.port.in.GetColumnaByIdUseCase;
import com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase;
import com.ar.crm2.application.ficha.port.in.GetFichaByIdUseCase;
import com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase;
import com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    void exposesExactlySixReadCallbacksWithoutTrustedIdentityOrDeleteOperations() {
        Map<String, ToolCallback> callbacks = Arrays.stream(ToolCallbacks.from(fixture(null)))
                .collect(Collectors.toMap(callback -> callback.getToolDefinition().name(), callback -> callback));

        assertThat(callbacks.keySet()).containsExactlyInAnyOrder(
                "list_tableros", "get_tablero", "list_columnas", "get_columna", "list_fichas", "get_ficha");
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
        SpringAiDevelopmentCrmTools tools = fixture(tableros);
        ToolContext context = trustedContext();

        TablerosOutput typed = tools.listTableros(context);
        assertThat(typed.tableros()).isEmpty();
        assertThat(typed.total()).isZero();
        assertThat(typed.truncated()).isFalse();

        ToolCallback callback = Arrays.stream(ToolCallbacks.from(tools))
                .filter(candidate -> candidate.getToolDefinition().name().equals("list_tableros"))
                .findFirst().orElseThrow();
        JsonNode serialized = MAPPER.readTree(callback.call("{}", context));
        assertThat(serialized.get("tableros")).isEmpty();
        assertThat(serialized.get("total").asInt()).isZero();
        assertThat(serialized.get("truncated").asBoolean()).isFalse();
    }

    @Test
    void everyReadRejectsAbsentTrustedActorBeforeDelegation() {
        Map<String, String> arguments = Map.of(
                "list_tableros", "{}",
                "get_tablero", "{\"id\":\"00000000-0000-0000-0000-000000000001\"}",
                "list_columnas", "{}",
                "get_columna", "{\"id\":\"00000000-0000-0000-0000-000000000001\"}",
                "list_fichas", "{}",
                "get_ficha", "{\"id\":\"00000000-0000-0000-0000-000000000001\"}");

        for (ToolCallback callback : ToolCallbacks.from(fixture(null))) {
            Throwable failure = catchThrowable(() -> callback.call(
                    arguments.get(callback.getToolDefinition().name()),
                    new ToolContext(Map.of("unrelatedTrustedContext", "present"))));
            assertThat(failure).isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
            assertThat(failure.getCause()).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void delegatesAllSixReadsToTheirApplicationPorts() {
        GetAllTablerosUseCase tableros = mock(GetAllTablerosUseCase.class);
        GetTableroByIdUseCase tablero = mock(GetTableroByIdUseCase.class);
        GetAllColumnasUseCase columnas = mock(GetAllColumnasUseCase.class);
        GetColumnaByIdUseCase columna = mock(GetColumnaByIdUseCase.class);
        GetAllFichasUseCase fichas = mock(GetAllFichasUseCase.class);
        GetFichaByIdUseCase ficha = mock(GetFichaByIdUseCase.class);
        when(tableros.getAll()).thenReturn(List.of());
        when(columnas.getAll()).thenReturn(List.of());
        when(fichas.getAll()).thenReturn(List.of());
        SpringAiDevelopmentCrmTools tools = new SpringAiDevelopmentCrmTools(
                tableros, tablero, columnas, columna, fichas, ficha);
        ToolContext context = trustedContext();

        tools.listTableros(context);
        tools.getTablero(UUID.randomUUID(), context);
        tools.listColumnas(context);
        tools.getColumna(UUID.randomUUID(), context);
        tools.listFichas(context);
        tools.getFicha(UUID.randomUUID(), context);

        verify(tableros).getAll();
        verify(tablero).getById(any());
        verify(columnas).getAll();
        verify(columna).getById(any());
        verify(fichas).getAll();
        verify(ficha).getById(any());
    }

    private static ToolContext trustedContext() {
        return new ToolContext(Map.of("actorUsuarioId", UUID.randomUUID()));
    }

    private static SpringAiDevelopmentCrmTools fixture(GetAllTablerosUseCase tableros) {
        return new SpringAiDevelopmentCrmTools(
                tableros == null ? mock(GetAllTablerosUseCase.class) : tableros,
                mock(GetTableroByIdUseCase.class),
                mock(GetAllColumnasUseCase.class),
                mock(GetColumnaByIdUseCase.class),
                mock(GetAllFichasUseCase.class),
                mock(GetFichaByIdUseCase.class));
    }
}
