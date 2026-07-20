package com.ar.crm2.adapter.out.persistence.agent.mapper;

import com.ar.crm2.adapter.out.persistence.agent.entity.AgentConversationEntity;
import com.ar.crm2.adapter.out.persistence.agent.entity.AgentTurnEntity;
import com.ar.crm2.model.agent.entity.AgentTurn;
import com.ar.crm2.model.agent.entity.Conversation;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.ConversationId;
import com.ar.crm2.model.agent.vo.TurnId;

import java.time.LocalDateTime;

public final class AgentTurnPersistenceMapper {

    private AgentTurnPersistenceMapper() {
    }

    public static AgentConversationEntity toEntity(Conversation conversation, LocalDateTime createdAt) {
        if (conversation == null) {
            return null;
        }
        return new AgentConversationEntity(
            conversation.getId().value().toString(),
            conversation.getOwnerId().value(),
            createdAt
        );
    }

    public static AgentTurnEntity toEntity(
            AgentTurn turn,
            AgentConversationEntity conversation,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        if (turn == null) {
            return null;
        }
        return new AgentTurnEntity(
            turn.getId().value().toString(),
            conversation,
            turn.getState(),
            createdAt,
            updatedAt
        );
    }

    public static Conversation toDomain(AgentConversationEntity entity) {
        if (entity == null) {
            return null;
        }
        return Conversation.reconstitute(
            ConversationId.from(java.util.UUID.fromString(entity.getId())),
            AgentOwnerId.from(entity.getOwnerId())
        );
    }

    public static AgentTurn toDomain(AgentTurnEntity entity) {
        if (entity == null) {
            return null;
        }
        return AgentTurn.reconstitute(
            TurnId.from(java.util.UUID.fromString(entity.getId())),
            ConversationId.from(java.util.UUID.fromString(entity.getConversation().getId())),
            entity.getState()
        );
    }
}
