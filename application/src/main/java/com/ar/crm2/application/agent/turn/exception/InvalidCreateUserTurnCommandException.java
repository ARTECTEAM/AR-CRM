package com.ar.crm2.application.agent.turn.exception;

/** Signals a structurally invalid create-user-turn command. */
public final class InvalidCreateUserTurnCommandException extends IllegalArgumentException {

    private InvalidCreateUserTurnCommandException(String message) {
        super(message);
    }

    public static InvalidCreateUserTurnCommandException required(String field) {
        return new InvalidCreateUserTurnCommandException(field + " is required");
    }
}
