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
    public DurableMemory save(DurableMemory memory) {
        return DurableMemoryPersistenceMapper.toDomain(repository.save(DurableMemoryPersistenceMapper.toEntity(memory)));
    }

    @Override
    @Transactional
    public DurableMemory replace(AgentOwnerId ownerId, MemoryId targetId, DurableMemory replacement) {
        DurableMemory original = findRequiredByOwnerAndId(ownerId, targetId);
        DurableMemory superseded = original.supersedeWith(replacement, ownerId, targetId);
        repository.save(DurableMemoryPersistenceMapper.toEntity(replacement));
        repository.save(DurableMemoryPersistenceMapper.toEntity(superseded));
        return replacement;
    }

    @Override
    @Transactional
    public void delete(AgentOwnerId ownerId, MemoryId targetId) {
        DurableMemory original = findRequiredByOwnerAndId(ownerId, targetId);
        repository.save(DurableMemoryPersistenceMapper.toEntity(original.delete(ownerId, targetId)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DurableMemory> findByOwnerAndId(AgentOwnerId ownerId, MemoryId memoryId) {
        return repository.findByOwnerIdAndId(ownerId.value(), memoryId.value().toString())
                .map(DurableMemoryPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DurableMemory> findEligible(AgentOwnerId ownerId) {
        LocalDateTime now = LocalDateTime.now();
        List<DurableMemoryEntity> memories = new ArrayList<>();
        memories.addAll(repository.findByOwnerIdAndStatusAndExpiresAtIsNullOrderByCreatedAtAscIdAsc(
                ownerId.value(), DurableMemoryStatus.ACTIVE));
        memories.addAll(repository.findByOwnerIdAndStatusAndExpiresAtAfterOrderByCreatedAtAscIdAsc(
                ownerId.value(), DurableMemoryStatus.ACTIVE, now));
        return memories.stream()
                .sorted(java.util.Comparator.comparing(DurableMemoryEntity::getCreatedAt).thenComparing(DurableMemoryEntity::getId))
                .map(DurableMemoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findEligibleDurableMemories(AgentOwnerId ownerId) {
        return findEligible(ownerId).stream().map(DurableMemory::getContent).toList();
    }

    @Override
    @Transactional
    public void purgeExpiredAndDeletedBefore(LocalDateTime retentionBoundary) {
        repository.deleteExpiredAndDeletedBefore(retentionBoundary, DurableMemoryStatus.ACTIVE, DurableMemoryStatus.DELETED);
    }

    private DurableMemory findRequiredByOwnerAndId(AgentOwnerId ownerId, MemoryId targetId) {
        return repository.findByOwnerIdAndIdForUpdate(ownerId.value(), targetId.value().toString())
                .map(DurableMemoryPersistenceMapper::toDomain)
                .orElseThrow();
    }
}
