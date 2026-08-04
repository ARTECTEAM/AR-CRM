package com.ar.crm2.application.agent.tool.exception;

import java.util.UUID;

/** Signals strict deal ownership denial before any CRM mutation. */
public final class DealNotOwnedByActorException extends RuntimeException {

    private final UUID tratoId;
    private final UUID actorUsuarioId;

    public DealNotOwnedByActorException(UUID tratoId, UUID actorUsuarioId) {
        super("Trato " + tratoId + " is not owned by actor " + actorUsuarioId);
        this.tratoId = tratoId;
        this.actorUsuarioId = actorUsuarioId;
    }

    public UUID tratoId() {
        return tratoId;
    }

    public UUID actorUsuarioId() {
        return actorUsuarioId;
    }
}
