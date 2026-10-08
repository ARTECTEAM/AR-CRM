package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.application.security.AuthorizationCapabilities;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable registry that derives a fresh callback subset for each turn's authorization snapshot. */
public final class AgentToolCallbackCatalog {
    private final Map<String, ToolCallback> callbacksByName;

    public AgentToolCallbackCatalog(ToolCallback... callbacks) {
        if (callbacks == null) {
            throw new IllegalArgumentException("Registered tool callbacks are required");
        }
        Map<String, ToolCallback> indexed = new LinkedHashMap<>();
        Arrays.stream(callbacks).forEach(callback -> {
            if (callback == null || callback.getToolDefinition() == null
                    || callback.getToolDefinition().name() == null
                    || callback.getToolDefinition().name().isBlank()) {
                throw new IllegalStateException("Every registered CRM tool must have a name");
            }
            String name = callback.getToolDefinition().name();
            if (indexed.putIfAbsent(name, callback) != null) {
                throw new IllegalStateException("Duplicate registered CRM tool: " + name);
            }
        });

        Set<String> expected = AgentToolPermissionPolicy.toolNames();
        if (!indexed.keySet().equals(expected)) {
            throw new IllegalStateException("Registered CRM tools and authorization mapping differ");
        }
        callbacksByName = Map.copyOf(indexed);
    }

    private AgentToolCallbackCatalog(Map<String, ToolCallback> callbacksByName) {
        this.callbacksByName = Map.copyOf(callbacksByName);
    }

    /** Fail-closed catalog for contexts that intentionally expose no tool callbacks. */
    public static AgentToolCallbackCatalog empty() {
        return new AgentToolCallbackCatalog(Map.of());
    }

    public List<ToolCallback> callbacksFor(AuthorizationCapabilities capabilities) {
        if (capabilities == null) {
            return List.of();
        }
        return AgentToolPermissionPolicy.allowedTools(capabilities).stream()
                .sorted()
                .map(callbacksByName::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public Set<String> registeredToolNames() {
        return callbacksByName.keySet();
    }

    public String describeCapabilities(AuthorizationCapabilities capabilities) {
        return AgentToolPermissionPolicy.describe(capabilities);
    }
}
