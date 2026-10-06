package com.ar.crm2.application.agent.turn.command;

import com.ar.crm2.application.agent.command.ApplicationAssert;

/** Simple inputs supplied by the REST adapter for a user turn. */
public record CreateUserTurnCommand(String actorSubject, String idempotencyKey, String prompt) {

    public CreateUserTurnCommand {
        actorSubject = ApplicationAssert.requiredTrimmed(actorSubject, "actorSubject");
        idempotencyKey = ApplicationAssert.requiredTrimmed(idempotencyKey, "idempotencyKey");
        prompt = ApplicationAssert.requiredTrimmed(prompt, "prompt");
    }
}
