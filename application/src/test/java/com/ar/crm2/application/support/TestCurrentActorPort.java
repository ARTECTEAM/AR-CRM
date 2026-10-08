package com.ar.crm2.application.support;

import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.port.out.CurrentActorPort;

import java.util.Optional;
import java.util.UUID;

public record TestCurrentActorPort(CurrentActor actor) implements CurrentActorPort {
    public TestCurrentActorPort(UUID usuarioId) {
        this(new CurrentActor(usuarioId, UUID.randomUUID(), false));
    }

    @Override
    public Optional<CurrentActor> currentActor() {
        return Optional.of(actor);
    }
}
