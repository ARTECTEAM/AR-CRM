package com.ar.crm2.adapter.out.persistence.agent.memory;

import com.ar.crm2.application.agent.memory.port.out.FindDurableMemoryByOwnerAndIdPort;
import com.ar.crm2.application.agent.memory.port.out.FindEligibleDurableMemoriesPort;
import com.ar.crm2.application.agent.memory.port.out.PurgeDurableMemoriesPort;
import com.ar.crm2.application.agent.memory.port.out.DeleteDurableMemoryPort;
import com.ar.crm2.application.agent.memory.port.out.ReplaceDurableMemoryPort;
import com.ar.crm2.application.agent.memory.port.out.SaveDurableMemoryPort;
import com.ar.crm2.model.agent.entity.DurableMemory;
import com.ar.crm2.model.agent.enums.DurableMemoryStatus;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.MemoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class DurableMemoryPersistenceAdapter implements SaveDurableMemoryPort, FindDurableMemoryByOwnerAndIdPort,
        FindEligibleDurableMemoriesPort, PurgeDurableMemoriesPort, ReplaceDurableMemoryPort, DeleteDurableMemoryPort,
        com.ar.crm2.application.agent.turn.port.out.FindEligibleDurableMemoriesPort {
    private final DurableMemoryRepository repository;

    @Override
    @Transactional
    public DurableMemory save(DurableMemory memory, String authorizationRevision) {
        return DurableMemoryPersistenceMapper.toDomain(repository.save(
                DurableMemoryPersistenceMapper.toEntity(memory, authorizationRevision)));
    }

    @Override
    @Transactional
    public DurableMemory replace(
            AgentOwnerId ownerId, MemoryId targetId, DurableMemory replacement, String authorizationRevision) {
        DurableMemoryEntity originalEntity = findRequiredEntityByOwnerAndId(ownerId, targetId);
        DurableMemory original = DurableMemoryPersistenceMapper.toDomain(originalEntity);
        DurableMemory superseded = original.supersedeWith(replacement, ownerId, targetId);
        repository.save(DurableMemoryPersistenceMapper.toEntity(replacement, authorizationRevision));
        repository.save(DurableMemoryPersistenceMapper.toEntity(superseded, originalEntity.getAuthorizationRevision()));
        return replacement;
    }

    @Override
    @Transactional
    public void delete(AgentOwnerId ownerId, MemoryId targetId) {
        DurableMemoryEntity originalEntity = findRequiredEntityByOwnerAndId(ownerId, targetId);
        DurableMemory original = DurableMemoryPersistenceMapper.toDomain(originalEntity);
        repository.save(DurableMemoryPersistenceMapper.toEntity(
                original.delete(ownerId, targetId), originalEntity.getAuthorizationRevision()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DurableMemory> findByOwnerAndId(AgentOwnerId ownerId, MemoryId memoryId) {
        return repository.findByOwnerIdAndId(ownerId.value(), memoryId.value().toString())
                .map(DurableMemoryPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DurableMemory> findEligible(AgentOwnerId ownerId, String authorizationRevision) {
        LocalDateTime now = LocalDateTime.now();
        List<DurableMemoryEntity> memories = new ArrayList<>();
        memories.addAll(repository.findByOwnerIdAndAuthorizationRevisionAndStatusAndExpiresAtIsNullOrderByCreatedAtAscIdAsc(
                ownerId.value(), authorizationRevision, DurableMemoryStatus.ACTIVE));
        memories.addAll(repository.findByOwnerIdAndAuthorizationRevisionAndStatusAndExpiresAtAfterOrderByCreatedAtAscIdAsc(
                ownerId.value(), authorizationRevision, DurableMemoryStatus.ACTIVE, now));
        return memories.stream()
                .sorted(java.util.Comparator.comparing(DurableMemoryEntity::getCreatedAt).thenComparing(DurableMemoryEntity::getId))
                .map(DurableMemoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findEligibleDurableMemories(AgentOwnerId ownerId, String authorizationRevision) {
        return findEligible(ownerId, authorizationRevision).stream().map(DurableMemory::getContent).toList();
    }

    @Override
    @Transactional
    public void purgeExpiredAndDeletedBefore(LocalDateTime retentionBoundary) {
        repository.deleteExpiredAndDeletedBefore(retentionBoundary, DurableMemoryStatus.ACTIVE, DurableMemoryStatus.DELETED);
    }

    private DurableMemoryEntity findRequiredEntityByOwnerAndId(AgentOwnerId ownerId, MemoryId targetId) {
        return repository.findByOwnerIdAndIdForUpdate(ownerId.value(), targetId.value().toString())
                .orElseThrow();
    }
}
