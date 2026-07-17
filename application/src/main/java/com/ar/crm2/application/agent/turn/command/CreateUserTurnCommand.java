package com.ar.crm2.application.agent.turn.command;

import com.ar.crm2.application.agent.turn.exception.InvalidCreateUserTurnCommandException;

/** Simple inputs supplied by the REST adapter for a user turn. */
public record CreateUserTurnCommand(String actorSubject, String idempotencyKey, String prompt) {

    public CreateUserTurnCommand {
        actorSubject = required(actorSubject, "actorSubject");
        idempotencyKey = required(idempotencyKey, "idempotencyKey");
        prompt = required(prompt, "prompt");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw InvalidCreateUserTurnCommandException.required(field);
        }
        return value.trim();
    }
}
