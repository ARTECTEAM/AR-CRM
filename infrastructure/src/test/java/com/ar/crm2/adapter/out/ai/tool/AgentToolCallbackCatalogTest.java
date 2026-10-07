package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.application.security.AuthorizationCapabilities;
import com.ar.crm2.application.security.ResourceCapabilities;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentToolCallbackCatalogTest {
    private static final List<Class<?>> TOOL_GROUPS = List.of(
            AgendaTools.class, ColumnaTools.class, ContactoTools.class,
            EmpresaTools.class, EtiquetaTools.class, FichaTools.class,
            TableroTools.class, TareaTools.class, TratoTools.class);

    @Test
    void derivesAnImmutableCallbackSubsetForEachSnapshotWithoutMutatingTheSharedCatalog() {
        AgentToolCallbackCatalog catalog = new AgentToolCallbackCatalog(mockCallbacks());
        AuthorizationCapabilities contactReads = new AuthorizationCapabilities(Map.of(
                RecursoCrm.CONTACTO, capability(AccionCrm.LEER),
                RecursoCrm.EMPRESA, capability(AccionCrm.LEER)));

        List<ToolCallback> none = catalog.callbacksFor(AuthorizationCapabilities.none());
        List<ToolCallback> allowed = catalog.callbacksFor(contactReads);

        assertThat(none).isEmpty();
        assertThat(names(allowed)).containsExactlyInAnyOrder("find_contacts", "get_contact", "list_companies");
        assertThatThrownBy(() -> allowed.clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(catalog.callbacksFor(null)).isEmpty();
        assertThat(catalog.callbacksFor(AuthorizationCapabilities.none())).isEmpty();
        assertThat(catalog.registeredToolNames()).hasSize(50);
    }

    @Test
    void rejectsMissingDuplicateAndUnnamedProductionCallbacksInsteadOfSilentlyShrinkingTheCatalog() {
        ToolCallback[] callbacks = mockCallbacks();

        ToolCallback[] unknownName = Arrays.copyOf(callbacks, callbacks.length);
        unknownName[0] = mockCallback("retired_or_unknown_tool");
        assertThatThrownBy(() -> new AgentToolCallbackCatalog(unknownName))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> new AgentToolCallbackCatalog(Arrays.copyOf(callbacks, callbacks.length - 1)))
                .isInstanceOf(IllegalStateException.class);
        ToolCallback[] duplicate = Arrays.copyOf(callbacks, callbacks.length + 1);
        duplicate[callbacks.length] = callbacks[0];
        assertThatThrownBy(() -> new AgentToolCallbackCatalog(duplicate))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new AgentToolCallbackCatalog(new ToolCallback[]{mock(ToolCallback.class)}))
                .isInstanceOf(IllegalStateException.class);
    }

    private static ResourceCapabilities capability(AccionCrm... actions) {
        return new ResourceCapabilities(AlcanceCrm.TODO_COMPARTIDO,
                EnumSet.copyOf(List.of(actions)), Set.of(), Set.of());
    }

    private static Set<String> names(List<ToolCallback> callbacks) {
        return callbacks.stream().map(callback -> callback.getToolDefinition().name()).collect(Collectors.toSet());
    }

    private static ToolCallback[] mockCallbacks() {
        return TOOL_GROUPS.stream()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .map(method -> method.getAnnotation(Tool.class))
                .filter(java.util.Objects::nonNull)
                .map(Tool::name)
                .map(AgentToolCallbackCatalogTest::mockCallback)
                .toArray(ToolCallback[]::new);
    }

    private static ToolCallback mockCallback(String name) {
        ToolCallback callback = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(callback.getToolDefinition()).thenReturn(definition);
        when(definition.name()).thenReturn(name);
        return callback;
    }
}
