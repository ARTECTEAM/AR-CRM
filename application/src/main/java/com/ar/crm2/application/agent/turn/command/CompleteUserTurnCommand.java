package com.ar.crm2.application.agent.turn.command;

import com.ar.crm2.application.shared.ApplicationAssert;

import java.util.UUID;

/** Simple trusted inputs supplied by the future agent adapter to complete a user turn. */
public record CompleteUserTurnCommand(
        String actorSubject,
        UUID turnId,
        String opaqueHandle,
        String prompt,
        int visibleHistoryLimit
) {

    public CompleteUserTurnCommand {
        actorSubject = ApplicationAssert.requiredTrimmed(actorSubject, "actorSubject");
        turnId = ApplicationAssert.required(turnId, "turnId");
        opaqueHandle = ApplicationAssert.requiredTrimmed(opaqueHandle, "opaqueHandle");
        prompt = ApplicationAssert.requiredTrimmed(prompt, "prompt");
        ApplicationAssert.positive(visibleHistoryLimit, "visibleHistoryLimit");
    }
}
