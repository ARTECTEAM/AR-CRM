package com.ar.crm2.adapter.out.persistence.agent.memory;

import com.ar.crm2.model.agent.entity.DurableMemory;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.MemoryId;

import java.util.UUID;

public final class DurableMemoryPersistenceMapper {
    private DurableMemoryPersistenceMapper() {
    }

    public static DurableMemoryEntity toEntity(DurableMemory memory) {
        return DurableMemoryEntity.builder()
                .id(memory.getId().value().toString())
                .ownerId(memory.getOwnerId().value())
                .content(memory.getContent())
                .status(memory.getStatus())
                .createdAt(memory.getCreatedAt())
                .updatedAt(memory.getUpdatedAt())
                .expiresAt(memory.getExpiresAt())
                .supersededAt(memory.getSupersededAt())
                .supersededById(memory.getSupersededById() == null
                        ? null : memory.getSupersededById().value().toString())
                .deletedAt(memory.getDeletedAt())
                .build();
    }

    public static DurableMemory toDomain(DurableMemoryEntity entity) {
        return DurableMemory.reconstitute(
                MemoryId.from(UUID.fromString(entity.getId())), AgentOwnerId.from(entity.getOwnerId()), entity.getContent(),
                entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getExpiresAt(), entity.getSupersededAt(),
                entity.getSupersededById() == null ? null : MemoryId.from(UUID.fromString(entity.getSupersededById())), entity.getDeletedAt());
    }
}
