package com.ar.crm2.adapter.out.ai.tool;

import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;
import java.util.UUID;

/** Reads trusted identity claims supplied by the server, never by model arguments. */
final class ToolContextSupport {

    static final String ACTOR_CONTEXT_KEY = "actorUsuarioId";
    static final String SUPER_USUARIO_CONTEXT_KEY = "actorSuperUsuarioId";

    private ToolContextSupport() {
    }

    static UUID requireActor(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            throw new IllegalStateException(ACTOR_CONTEXT_KEY + " is required");
        }
        Object raw = toolContext.getContext().get(ACTOR_CONTEXT_KEY);
        if (raw == null) {
            throw new IllegalStateException(ACTOR_CONTEXT_KEY + " is required");
        }
        if (!(raw instanceof UUID uuid)) {
            throw new IllegalArgumentException(ACTOR_CONTEXT_KEY + " must be a UUID");
        }
        return uuid;
    }

    static UUID trustedOptionalUuid(ToolContext toolContext, String key) {
        Map<String, Object> context = toolContext == null ? null : toolContext.getContext();
        Object raw = context == null ? null : context.get(key);
        if (raw == null) return null;
        if (raw instanceof UUID uuid) return uuid;
        throw new IllegalArgumentException("Trusted identity context has an invalid type");
    }
}
