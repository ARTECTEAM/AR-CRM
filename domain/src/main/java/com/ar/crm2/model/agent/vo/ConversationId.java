package com.ar.crm2.model.agent.vo;

import com.ar.crm2.shared.DomainAssert;

import java.util.UUID;

/**
 * Immutable server-owned conversation identity.
 */
public record ConversationId(UUID value) {

    public ConversationId {
        DomainAssert.notNull(value, "conversationId");
    }

    public static ConversationId from(UUID value) {
        return new ConversationId(value);
    }

    public static ConversationId create() {
        return new ConversationId(UUID.randomUUID());
    }
}
