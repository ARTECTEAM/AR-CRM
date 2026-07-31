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
        UUID actorUsuarioId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        CompleteUserTurnCommand command = new CompleteUserTurnCommand(
                " actor-a ", actorUsuarioId, turnId, " handle ", " prompt ", 12
        );

        assertEquals("actor-a", command.actorSubject());
        assertEquals(actorUsuarioId, command.actorUsuarioId());
        assertEquals(turnId, command.turnId());
        assertEquals("handle", command.opaqueHandle());
        assertEquals("prompt", command.prompt());
        assertEquals(12, command.visibleHistoryLimit());
    }

    @Test
    void completePreservesActorUsuarioIdRegardlessOfOwnerSubjectValue() {
        UUID turnId = UUID.randomUUID();
        UUID actorUsuarioId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        CompleteUserTurnCommand command = new CompleteUserTurnCommand(
                "owner-subject-not-crm-id", actorUsuarioId, turnId, "handle", "prompt", 5
        );

        assertEquals(actorUsuarioId, command.actorUsuarioId());
        assertEquals("owner-subject-not-crm-id", command.actorSubject());
    }

    @Test
    void completeRejectsMissingActorUsuarioIdWithExactMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new CompleteUserTurnCommand(
                        "actor-a", null, UUID.randomUUID(), "handle", "prompt", 12));

        assertEquals("actorUsuarioId is required", exception.getMessage());
    }

    @Test
    void completeRejectsMissingTurnIdentityWithExactMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new CompleteUserTurnCommand(
                        "actor-a", UUID.randomUUID(), null, "handle", "prompt", 12));

        assertEquals("turnId is required", exception.getMessage());
    }

    @Test
    void completeRejectsNonPositiveVisibleHistoryLimitWithExactMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new CompleteUserTurnCommand(
                        "actor-a", UUID.randomUUID(), UUID.randomUUID(), "handle", "prompt", 0));

        assertEquals("visibleHistoryLimit must be positive", exception.getMessage());
    }

    @Test
    void regenerateNormalizesTextAndPreservesTrustedTurnIdentity() {
        UUID turnId = UUID.randomUUID();
        UUID actorUsuarioId = UUID.fromString("22222222-3333-4444-5555-666666666666");
        RegenerateUserTurnCommand command = new RegenerateUserTurnCommand(
                " actor-a ", actorUsuarioId, turnId, " handle ", " key-1 ", 12
        );

        assertEquals("actor-a", command.actorSubject());
        assertEquals(actorUsuarioId, command.actorUsuarioId());
        assertEquals(turnId, command.turnId());
        assertEquals("handle", command.opaqueHandle());
        assertEquals("key-1", command.idempotencyKey());
        assertEquals(12, command.visibleHistoryLimit());
    }

    @Test
    void regenerateRejectsMissingActorUsuarioIdWithExactMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new RegenerateUserTurnCommand(
                        "actor-a", null, UUID.randomUUID(), "handle", "key-1", 12));

        assertEquals("actorUsuarioId is required", exception.getMessage());
    }

    @Test
    void regenerateRejectsMissingRequiredTextWithExactMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new RegenerateUserTurnCommand(
                        "actor-a", UUID.randomUUID(), UUID.randomUUID(), " ", "key-1", 12));

        assertEquals("opaqueHandle is required", exception.getMessage());
    }

    @Test
    void regenerateRejectsNonPositiveVisibleHistoryLimitWithExactMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new RegenerateUserTurnCommand(
                        "actor-a", UUID.randomUUID(), UUID.randomUUID(), "handle", "key-1", -1));

        assertEquals("visibleHistoryLimit must be positive", exception.getMessage());
    }
}
