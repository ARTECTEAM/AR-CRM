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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentToolCallbackCatalogTest {

    @Test
    void derivesAnImmutableCallbackSubsetPerSnapshotWithoutMutatingTheSharedCatalog() {
        AgentToolCallbackCatalog catalog = new AgentToolCallbackCatalog(mockCallbacks());
        AuthorizationCapabilities noCapabilities = AuthorizationCapabilities.none();
        AuthorizationCapabilities contactReads = new AuthorizationCapabilities(Map.of(
                RecursoCrm.CONTACTO, capability(AccionCrm.LEER),
                RecursoCrm.EMPRESA, capability(AccionCrm.LEER)));

        assertEquals(List.of(), catalog.callbacksFor(noCapabilities));
        List<ToolCallback> allowed = catalog.callbacksFor(contactReads);
        assertEquals(Set.of("find_contacts", "get_contact", "list_companies"), names(allowed));
        assertThrows(UnsupportedOperationException.class, () -> allowed.clear());
        assertEquals(List.of(), catalog.callbacksFor(noCapabilities));
    }

    @Test
    void failsClosedWhenTheRegisteredCallbacksDoNotExactlyMatchThePermissionMap() {
        assertThrows(IllegalStateException.class,
                () -> new AgentToolCallbackCatalog(new ToolCallback[0]));
    }

    private static ResourceCapabilities capability(AccionCrm... actions) {
        return new ResourceCapabilities(AlcanceCrm.TODO_COMPARTIDO,
                EnumSet.copyOf(List.of(actions)), Set.of(), Set.of());
    }

    private static Set<String> names(List<ToolCallback> callbacks) {
        return callbacks.stream().map(callback -> callback.getToolDefinition().name()).collect(Collectors.toSet());
    }

    private static ToolCallback[] mockCallbacks() {
        return Stream.of(AgendaTools.class, ColumnaTools.class, ContactoTools.class,
                        EmpresaTools.class, EtiquetaTools.class, FichaTools.class,
                        TableroTools.class, TareaTools.class, TratoTools.class)
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .map(method -> method.getAnnotation(Tool.class))
                .filter(tool -> tool != null)
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
