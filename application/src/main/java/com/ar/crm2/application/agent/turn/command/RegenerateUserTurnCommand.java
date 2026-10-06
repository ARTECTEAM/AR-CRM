package com.ar.crm2.application.agent.turn.command;

import com.ar.crm2.application.shared.ApplicationAssert;

import java.util.UUID;

/**
 * Simple trusted inputs supplied by the future agent adapter to regenerate a user turn.
 *
 * <p>Carries the same trusted CRM {@code actorUsuarioId} as
 * {@link CompleteUserTurnCommand}; the trusted actor identity is
 * established at the agent ingress before either command is constructed,
 * and it is forwarded unchanged through chat completion.
 */
public record RegenerateUserTurnCommand(
        String actorSubject,
        UUID actorUsuarioId,
        UUID turnId,
        String opaqueHandle,
        String idempotencyKey,
        int visibleHistoryLimit
) {

    public RegenerateUserTurnCommand {
        actorSubject = ApplicationAssert.requiredTrimmed(actorSubject, "actorSubject");
        actorUsuarioId = ApplicationAssert.required(actorUsuarioId, "actorUsuarioId");
        turnId = ApplicationAssert.required(turnId, "turnId");
        opaqueHandle = ApplicationAssert.requiredTrimmed(opaqueHandle, "opaqueHandle");
        idempotencyKey = ApplicationAssert.requiredTrimmed(idempotencyKey, "idempotencyKey");
        ApplicationAssert.positive(visibleHistoryLimit, "visibleHistoryLimit");
    }
}
