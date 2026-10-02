package com.ar.crm2.application.security;

import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Immutable role-level authorization snapshot for model tool selection. */
public record AuthorizationCapabilities(Map<RecursoCrm, ResourceCapabilities> resources) {
    public AuthorizationCapabilities {
        EnumMap<RecursoCrm, ResourceCapabilities> copy = new EnumMap<>(RecursoCrm.class);
        if (resources != null) {
            resources.forEach((resource, capabilities) -> {
                if (resource != null && capabilities != null) {
                    copy.put(resource, capabilities);
                }
            });
        }
        resources = Map.copyOf(copy);
    }

    public static AuthorizationCapabilities none() {
        return new AuthorizationCapabilities(Map.of());
    }

    public Optional<ResourceCapabilities> forResource(RecursoCrm resource) {
        return Optional.ofNullable(resources.get(resource));
    }

    public boolean permits(RecursoCrm resource, AccionCrm action) {
        return forResource(resource).map(capability -> capability.permits(action)).orElse(false);
    }
}
