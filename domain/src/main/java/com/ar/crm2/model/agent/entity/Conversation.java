package com.ar.crm2.model.agent.entity;

import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.ConversationId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.shared.DomainAssert;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Owner-bound conversation aggregate.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Conversation {

    @EqualsAndHashCode.Include
    private final ConversationId id;
    private final AgentOwnerId ownerId;

    public static Conversation create(AgentOwnerId ownerId) {
        DomainAssert.notNull(ownerId, "agentOwnerId");
        return new Conversation(ConversationId.create(), ownerId);
    }

    public static Conversation reconstitute(ConversationId id, AgentOwnerId ownerId) {
        DomainAssert.notNull(id, "conversationId");
        DomainAssert.notNull(ownerId, "agentOwnerId");
        return new Conversation(id, ownerId);
    }

    public AgentTurn prepareTurn(TurnId turnId) {
        DomainAssert.notNull(turnId, "turnId");
        return AgentTurn.prepare(turnId, id);
    }
}
