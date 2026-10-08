package com.ar.crm2.model.agent.vo;

import com.ar.crm2.shared.DomainAssert;

import java.util.UUID;

/**
 * Immutable server-owned turn identity.
 */
public record TurnId(UUID value) {

    public TurnId {
        DomainAssert.notNull(value, "turnId");
    }

    public static TurnId from(UUID value) {
        return new TurnId(value);
    }

    public static TurnId create() {
        return new TurnId(UUID.randomUUID());
    }
}
