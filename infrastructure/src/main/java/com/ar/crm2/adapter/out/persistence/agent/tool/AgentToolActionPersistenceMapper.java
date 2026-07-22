package com.ar.crm2.adapter.out.persistence.agent.tool;

import com.ar.crm2.model.agent.tool.entity.AgentToolAction;
import com.ar.crm2.model.agent.tool.vo.AgentToolActionId;
import com.ar.crm2.model.agent.tool.vo.AgentToolName;
import com.ar.crm2.model.agent.tool.vo.AgentToolResource;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;

import java.util.UUID;

public final class AgentToolActionPersistenceMapper {

    private AgentToolActionPersistenceMapper() {
    }

    public static AgentToolActionEntity toEntity(AgentToolAction action) {
        return toEntity(action, null);
    }

    static AgentToolActionEntity toEntity(AgentToolAction action, Long version) {
        AgentToolResource resource = action.getResource();
        return AgentToolActionEntity.builder()
                .id(action.getId().value())
                .version(version)
                .ownerId(action.getOwnerId().value())
                .turnId(action.getTurnId().value().toString())
                .toolName(action.getToolName().storageName())
                .canonicalArguments(action.getCanonicalArguments())
                .status(action.getStatus())
                .resourceType(resource == null ? null : resource.resourceType())
                .resourceId(resource == null ? null : resource.resourceId())
                .createdAt(action.getCreatedAt())
                .completedAt(action.getCompletedAt())
                .build();
    }

    public static AgentToolAction toDomain(AgentToolActionEntity entity) {
        AgentToolResource resource = entity.getResourceType() == null && entity.getResourceId() == null
                ? null
                : new AgentToolResource(entity.getResourceType(), entity.getResourceId());
        return AgentToolAction.reconstitute(
                new AgentToolActionId(entity.getId()),
                AgentOwnerId.from(entity.getOwnerId()),
                TurnId.from(UUID.fromString(entity.getTurnId())),
                AgentToolName.fromStorageName(entity.getToolName()),
                entity.getCanonicalArguments(),
                entity.getStatus(),
                resource,
                entity.getCreatedAt(),
                entity.getCompletedAt());
    }
}
