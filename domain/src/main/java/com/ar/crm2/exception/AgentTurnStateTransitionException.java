package com.ar.crm2.exception;

import com.ar.crm2.model.agent.enums.TurnState;

/**
 * Exception thrown when an agent turn lifecycle transition is invalid.
 */
public class AgentTurnStateTransitionException extends DomainException {

    private AgentTurnStateTransitionException(String message) {
        super(message);
    }

    public static AgentTurnStateTransitionException from(TurnState currentState, TurnState targetState) {
        return new AgentTurnStateTransitionException(
                "No se puede cambiar un turno de agente desde " + currentState + " a " + targetState + "."
        );
    }
}
