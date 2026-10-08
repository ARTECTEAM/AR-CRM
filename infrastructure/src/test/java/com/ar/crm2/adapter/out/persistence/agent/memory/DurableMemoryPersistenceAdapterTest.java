package com.ar.crm2.adapter.out.persistence.agent.memory;

import com.ar.crm2.model.agent.entity.DurableMemory;
import com.ar.crm2.model.agent.enums.DurableMemoryStatus;
import com.ar.crm2.model.agent.policy.MemorySafetyContext;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.MemoryId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(DurableMemoryPersistenceAdapter.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:durable-memory;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DurableMemoryPersistenceAdapterTest {

    @Autowired
    private DurableMemoryPersistenceAdapter adapter;

    @Autowired
    private DurableMemoryRepository repository;

    @AfterEach
    void clearPersistenceState() {
        repository.deleteAll();
    }

    @Test
    void recallsOnlyActiveUnexpiredOwnerMemoriesInCreatedAtThenIdOrderAndReconstitutesState() {
        AgentOwnerId owner = AgentOwnerId.from("owner-a");
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 20, 10, 0);
        DurableMemory first = memory(owner, "First", createdAt, UUID.fromString("00000000-0000-0000-0000-000000000001"), null, DurableMemoryStatus.ACTIVE, null);
        DurableMemory second = memory(owner, "Second", createdAt, UUID.fromString("00000000-0000-0000-0000-000000000002"), null, DurableMemoryStatus.ACTIVE, null);
        DurableMemory expired = memory(owner, "Expired", createdAt.minusDays(2), UUID.randomUUID(), createdAt.minusDays(1), DurableMemoryStatus.ACTIVE, null);
        DurableMemory otherOwner = memory(AgentOwnerId.from("owner-b"), "Other", createdAt, UUID.randomUUID(), null, DurableMemoryStatus.ACTIVE, null);
        adapter.save(second);
        adapter.save(expired);
        adapter.save(otherOwner);
        adapter.save(first);

        List<DurableMemory> recalled = adapter.findEligible(owner);

        assertThat(recalled).extracting(DurableMemory::getContent).containsExactly("First", "Second");
        assertThat(recalled.getFirst().getId()).isEqualTo(first.getId());
        assertThat(recalled.getFirst().getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void isolatesFindAndLifecycleWritesByOwnerAndPurgesExpiredOrDeletedRecordsAfterRetention() {
        AgentOwnerId ownerA = AgentOwnerId.from("owner-a");
        AgentOwnerId ownerB = AgentOwnerId.from("owner-b");
        LocalDateTime old = LocalDateTime.of(2026, 7, 1, 10, 0);
        DurableMemory ownerAMemory = memory(ownerA, "A", old, UUID.randomUUID(), null, DurableMemoryStatus.ACTIVE, null);
        DurableMemory ownerBMemory = memory(ownerB, "B", old, UUID.randomUUID(), null, DurableMemoryStatus.ACTIVE, null);
        DurableMemory deleted = DurableMemory.reconstitute(MemoryId.from(UUID.randomUUID()), ownerA, "Deleted",
                DurableMemoryStatus.DELETED, old, old, null, null, null, old);
        adapter.save(ownerAMemory);
        adapter.save(ownerBMemory);
        adapter.save(deleted);

        assertThat(adapter.findByOwnerAndId(ownerB, ownerAMemory.getId())).isEmpty();
        adapter.save(ownerAMemory.delete(ownerA, ownerAMemory.getId()));
        adapter.purgeExpiredAndDeletedBefore(old.plusDays(1));

        assertThat(adapter.findEligible(ownerA)).isEmpty();
        assertThat(adapter.findEligible(ownerB)).containsExactly(ownerBMemory);
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void atomicallyReplacesOnlyTheExplicitOwnerBoundActiveMemory() {
        AgentOwnerId ownerA = AgentOwnerId.from("owner-a");
        AgentOwnerId ownerB = AgentOwnerId.from("owner-b");
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 20, 10, 0);
        DurableMemory original = memory(ownerA, "Original", createdAt, UUID.randomUUID(), null, DurableMemoryStatus.ACTIVE, null);
        DurableMemory otherOwner = memory(ownerB, "Other", createdAt, UUID.randomUUID(), null, DurableMemoryStatus.ACTIVE, null);
        DurableMemory replacement = DurableMemory.create(ownerA, "Replacement", MemorySafetyContext.explicitSafe());
        adapter.save(original);
        adapter.save(otherOwner);

        DurableMemory savedReplacement = adapter.replace(ownerA, original.getId(), replacement);

        assertThat(savedReplacement).isEqualTo(replacement);
        assertThat(adapter.findByOwnerAndId(ownerA, original.getId()))
                .hasValueSatisfying(memory -> {
                    assertThat(memory.getStatus()).isEqualTo(DurableMemoryStatus.SUPERSEDED);
                    assertThat(memory.getSupersededById()).isEqualTo(replacement.getId());
                });
        assertThat(adapter.findEligible(ownerA)).containsExactly(replacement);
        assertThat(adapter.findEligible(ownerB)).containsExactly(otherOwner);
    }

    @Test
    void atomicallyDeletesOnlyTheExplicitOwnerBoundActiveMemory() {
        AgentOwnerId ownerA = AgentOwnerId.from("owner-a");
        AgentOwnerId ownerB = AgentOwnerId.from("owner-b");
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 20, 10, 0);
        DurableMemory ownerAMemory = memory(ownerA, "A", createdAt, UUID.randomUUID(), null, DurableMemoryStatus.ACTIVE, null);
        DurableMemory ownerBMemory = memory(ownerB, "B", createdAt, UUID.randomUUID(), null, DurableMemoryStatus.ACTIVE, null);
        adapter.save(ownerAMemory);
        adapter.save(ownerBMemory);

        adapter.delete(ownerA, ownerAMemory.getId());

        assertThat(adapter.findByOwnerAndId(ownerA, ownerAMemory.getId()))
                .hasValueSatisfying(memory -> {
                    assertThat(memory.getStatus()).isEqualTo(DurableMemoryStatus.DELETED);
                    assertThat(memory.getDeletedAt()).isNotNull();
                });
        assertThat(adapter.findEligible(ownerA)).isEmpty();
        assertThat(adapter.findEligible(ownerB)).containsExactly(ownerBMemory);
    }

    @Test
    void purgesAnActiveMemoryExpiredAtTheBoundaryWhileRetainingUnexpiredAndNotYetRetentionAgedRecords() {
        AgentOwnerId owner = AgentOwnerId.from("owner-a");
        LocalDateTime boundary = LocalDateTime.of(2026, 7, 20, 10, 0);
        DurableMemory expiredAtBoundary = memory(owner, "Expired", boundary.minusDays(1), UUID.randomUUID(),
                boundary, DurableMemoryStatus.ACTIVE, null);
        DurableMemory unexpired = memory(owner, "Unexpired", boundary.minusDays(1), UUID.randomUUID(),
                boundary.plusSeconds(1), DurableMemoryStatus.ACTIVE, null);
        DurableMemory deletedAtBoundary = memory(owner, "Deleted", boundary.minusDays(1), UUID.randomUUID(),
                null, DurableMemoryStatus.DELETED, boundary);
        adapter.save(expiredAtBoundary);
        adapter.save(unexpired);
        adapter.save(deletedAtBoundary);

        adapter.purgeExpiredAndDeletedBefore(boundary);

        assertThat(repository.findById(expiredAtBoundary.getId().value().toString())).isEmpty();
        assertThat(repository.findById(unexpired.getId().value().toString())).isPresent();
        assertThat(repository.findById(deletedAtBoundary.getId().value().toString())).isPresent();
    }

    @Test
    @Transactional
    void locksTheExplicitOwnerBoundTargetForAnAtomicLifecycleWrite() {
        AgentOwnerId owner = AgentOwnerId.from("owner-a");
        DurableMemory memory = DurableMemory.create(owner, "Memory", MemorySafetyContext.explicitSafe());
        adapter.save(memory);

        assertThat(repository.findByOwnerIdAndIdForUpdate(owner.value(), memory.getId().value().toString()))
                .isPresent();
    }

    private DurableMemory memory(AgentOwnerId owner, String content, LocalDateTime createdAt, UUID id,
                                 LocalDateTime expiresAt, DurableMemoryStatus status, LocalDateTime deletedAt) {
        return DurableMemory.reconstitute(MemoryId.from(id), owner, content, status, createdAt, createdAt,
                expiresAt, null, null, deletedAt);
    }
}
