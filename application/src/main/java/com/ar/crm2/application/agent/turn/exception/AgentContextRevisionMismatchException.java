package com.ar.crm2.application.agent.turn.exception;

/** A persisted turn cannot be reused after the owner's active CRM permissions changed. */
public final class AgentContextRevisionMismatchException extends RuntimeException {
    public AgentContextRevisionMismatchException() {
        super("This agent response cannot be reused after a CRM permission change; start a new turn");
    }
}
