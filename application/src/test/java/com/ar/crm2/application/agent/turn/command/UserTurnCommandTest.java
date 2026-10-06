package com.ar.crm2.application.agent.turn.command;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTurnCommandTest {

    @Test
    void createNormalizesAllRequiredTextFields() {
        CreateUserTurnCommand command = new CreateUserTurnCommand(" actor-a ", " key-1 ", " prompt ");

        assertEquals("actor-a", command.actorSubject());
        assertEquals("key-1", command.idempotencyKey());
        assertEquals("prompt", command.prompt());
    }

    @Test
    void createRejectsMissingRequiredTextWithExactMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new CreateUserTurnCommand(null, "key-1", "prompt"));

        assertEquals("actorSubject is required", exception.getMessage());
    }

    @Test
    void completeNormalizesTextAndPreservesTrustedTurnIdentity() {
        UUID turnId = UUID.randomUUID();
        CompleteUserTurnCommand command = new CompleteUserTurnCommand(
                " actor-a ", turnId, " handle ", " prompt ", 12
        );

        assertEquals("actor-a", command.actorSubject());
        assertEquals(turnId, command.turnId());
        assertEquals("handle", command.opaqueHandle());
        assertEquals("prompt", command.prompt());
        assertEquals(12, command.visibleHistoryLimit());
    }

    @Test
    void completeRejectsMissingTurnIdentityWithExactMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new CompleteUserTurnCommand("actor-a", null, "handle", "prompt", 12));

        assertEquals("turnId is required", exception.getMessage());
    }

    @Test
    void completeRejectsNonPositiveVisibleHistoryLimitWithExactMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new CompleteUserTurnCommand("actor-a", UUID.randomUUID(), "handle", "prompt", 0));

        assertEquals("visibleHistoryLimit must be positive", exception.getMessage());
    }

    @Test
    void regenerateNormalizesTextAndPreservesTrustedTurnIdentity() {
        UUID turnId = UUID.randomUUID();
        RegenerateUserTurnCommand command = new RegenerateUserTurnCommand(
                " actor-a ", turnId, " handle ", " key-1 ", 12
        );

        assertEquals("actor-a", command.actorSubject());
        assertEquals(turnId, command.turnId());
        assertEquals("handle", command.opaqueHandle());
        assertEquals("key-1", command.idempotencyKey());
        assertEquals(12, command.visibleHistoryLimit());
    }

    @Test
    void regenerateRejectsMissingRequiredTextWithExactMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new RegenerateUserTurnCommand("actor-a", UUID.randomUUID(), " ", "key-1", 12));

        assertEquals("opaqueHandle is required", exception.getMessage());
    }

    @Test
    void regenerateRejectsNonPositiveVisibleHistoryLimitWithExactMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new RegenerateUserTurnCommand("actor-a", UUID.randomUUID(), "handle", "key-1", -1));

        assertEquals("visibleHistoryLimit must be positive", exception.getMessage());
    }
}
