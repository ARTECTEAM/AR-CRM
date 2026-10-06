package com.ar.crm2.application.agent.turn.command;

import com.ar.crm2.application.agent.command.ApplicationAssert;

import java.util.UUID;

/** Simple trusted inputs supplied by the future agent adapter to regenerate a user turn. */
public record RegenerateUserTurnCommand(
        String actorSubject,
        UUID turnId,
        String opaqueHandle,
        String idempotencyKey,
        int visibleHistoryLimit
) {

    public RegenerateUserTurnCommand {
        actorSubject = ApplicationAssert.requiredTrimmed(actorSubject, "actorSubject");
        turnId = ApplicationAssert.required(turnId, "turnId");
        opaqueHandle = ApplicationAssert.requiredTrimmed(opaqueHandle, "opaqueHandle");
        idempotencyKey = ApplicationAssert.requiredTrimmed(idempotencyKey, "idempotencyKey");
        ApplicationAssert.positive(visibleHistoryLimit, "visibleHistoryLimit");
    }
}
