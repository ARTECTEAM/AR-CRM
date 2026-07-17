package com.ar.crm2.model.agent.entity;

import com.ar.crm2.exception.AgentTurnStateTransitionException;
import com.ar.crm2.model.agent.enums.TurnState;
import com.ar.crm2.model.agent.vo.ConversationId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.shared.DomainAssert;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Immutable turn lifecycle constrained to PREPARED then COMPLETED.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentTurn {

    @EqualsAndHashCode.Include
    private final TurnId id;
    private final ConversationId conversationId;
    private final TurnState state;

    static AgentTurn create(TurnId id, ConversationId conversationId) {
        DomainAssert.notNull(id, "turnId");
        DomainAssert.notNull(conversationId, "conversationId");
        return new AgentTurn(id, conversationId, TurnState.PREPARED);
    }

    public static AgentTurn reconstitute(TurnId id, ConversationId conversationId, TurnState state) {
        DomainAssert.notNull(id, "turnId");
        DomainAssert.notNull(conversationId, "conversationId");
        DomainAssert.notNull(state, "turnState");
        return new AgentTurn(id, conversationId, state);
    }

    public AgentTurn complete() {
        if (state != TurnState.PREPARED) {
            throw AgentTurnStateTransitionException.from(state, TurnState.COMPLETED);
        }
        return new AgentTurn(id, conversationId, TurnState.COMPLETED);
    }
}
