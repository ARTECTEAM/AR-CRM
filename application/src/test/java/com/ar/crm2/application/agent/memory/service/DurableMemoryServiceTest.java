package com.ar.crm2.application.agent.memory.service;

import com.ar.crm2.application.agent.memory.command.DeleteDurableMemoryCommand;
import com.ar.crm2.application.agent.memory.command.PurgeDurableMemoriesCommand;
import com.ar.crm2.application.agent.memory.command.RecallDurableMemoriesCommand;
import com.ar.crm2.application.agent.memory.command.RememberDurableMemoryCommand;
import com.ar.crm2.application.agent.memory.command.ReplaceDurableMemoryCommand;
import com.ar.crm2.application.agent.memory.port.out.DeleteDurableMemoryPort;
import com.ar.crm2.application.agent.memory.port.out.FindEligibleDurableMemoriesPort;
import com.ar.crm2.application.agent.memory.port.out.PurgeDurableMemoriesPort;
import com.ar.crm2.application.agent.memory.port.out.ReplaceDurableMemoryPort;
import com.ar.crm2.application.agent.memory.port.out.SaveDurableMemoryPort;
import com.ar.crm2.model.agent.entity.DurableMemory;
import com.ar.crm2.model.agent.policy.MemorySafetyContext;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.MemoryId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurableMemoryServiceTest {

    @Test
    void remembersOnlyAnExplicitSafeMemoryForItsOwner() {
        CapturingSavePort savePort = new CapturingSavePort();
        RememberDurableMemoryService service = new RememberDurableMemoryService(savePort);

        DurableMemory remembered = service.remember(new RememberDurableMemoryCommand(
                "owner-a", "Use Spanish", MemorySafetyContext.explicitSafe()));

        assertEquals(AgentOwnerId.from("owner-a"), remembered.getOwnerId());
        assertEquals("Use Spanish", remembered.getContent());
        assertEquals(List.of(remembered), savePort.saved);
    }

    @Test
    void rejectsUnsafeMemoryWithoutPersistingIt() {
        CapturingSavePort savePort = new CapturingSavePort();
        RememberDurableMemoryService service = new RememberDurableMemoryService(savePort);

        assertThrows(RuntimeException.class, () -> service.remember(new RememberDurableMemoryCommand(
                "owner-a", "secret", MemorySafetyContext.secret())));

        assertEquals(List.of(), savePort.saved);
    }

    @Test
    void replacesOnlyTheExplicitOwnerBoundMemoryThroughTheAtomicPort() {
        CapturingReplacePort replacePort = new CapturingReplacePort();
        ReplaceDurableMemoryService service = new ReplaceDurableMemoryService(replacePort);
        MemoryId targetId = MemoryId.create();

        DurableMemory replacement = service.replace(new ReplaceDurableMemoryCommand(
                "owner-a", targetId.value(), "Use English", MemorySafetyContext.explicitSafe()));

        assertEquals(AgentOwnerId.from("owner-a"), replacePort.ownerId);
        assertEquals(targetId, replacePort.targetId);
        assertEquals(replacement, replacePort.replacement);
        assertEquals("Use English", replacement.getContent());
    }

    @Test
    void deletesOnlyTheExplicitOwnerBoundMemoryThroughTheAtomicPort() {
        CapturingDeletePort deletePort = new CapturingDeletePort();
        DeleteDurableMemoryService service = new DeleteDurableMemoryService(deletePort);
        MemoryId targetId = MemoryId.create();

        service.delete(new DeleteDurableMemoryCommand("owner-a", targetId.value()));

        assertEquals(AgentOwnerId.from("owner-a"), deletePort.ownerId);
        assertEquals(targetId, deletePort.targetId);
    }

    @Test
    void recallsEligibleMemoriesInThePortProvidedOwnerScopedOrder() {
        CapturingEligiblePort findEligiblePort = new CapturingEligiblePort();
        RecallDurableMemoriesService service = new RecallDurableMemoriesService(findEligiblePort);
        AgentOwnerId ownerId = AgentOwnerId.from("owner-a");
        DurableMemory first = DurableMemory.create(ownerId, "First", MemorySafetyContext.explicitSafe());
        DurableMemory second = DurableMemory.create(ownerId, "Second", MemorySafetyContext.explicitSafe());
        findEligiblePort.eligible = List.of(first, second);

        List<DurableMemory> recalled = service.recall(new RecallDurableMemoriesCommand("owner-a"));

        assertEquals(ownerId, findEligiblePort.requestedOwnerId);
        assertEquals(List.of(first, second), recalled);
    }

    @Test
    void purgesExpiredAndDeletedRecordsAtTheSuppliedRetentionBoundary() {
        CapturingPurgePort purgePort = new CapturingPurgePort();
        PurgeDurableMemoriesService service = new PurgeDurableMemoriesService(purgePort);
        LocalDateTime retentionBoundary = LocalDateTime.of(2026, 7, 20, 10, 0);

        service.purge(new PurgeDurableMemoriesCommand(retentionBoundary));

        assertEquals(retentionBoundary, purgePort.boundary);
    }

    private static final class CapturingSavePort implements SaveDurableMemoryPort {
        private final java.util.ArrayList<DurableMemory> saved = new java.util.ArrayList<>();

        @Override
        public DurableMemory save(DurableMemory memory) {
            saved.add(memory);
            return memory;
        }
    }

    private static final class CapturingReplacePort implements ReplaceDurableMemoryPort {
        private AgentOwnerId ownerId;
        private MemoryId targetId;
        private DurableMemory replacement;

        @Override
        public DurableMemory replace(AgentOwnerId ownerId, MemoryId targetId, DurableMemory replacement) {
            this.ownerId = ownerId;
            this.targetId = targetId;
            this.replacement = replacement;
            return replacement;
        }
    }

    private static final class CapturingDeletePort implements DeleteDurableMemoryPort {
        private AgentOwnerId ownerId;
        private MemoryId targetId;

        @Override
        public void delete(AgentOwnerId ownerId, MemoryId targetId) {
            this.ownerId = ownerId;
            this.targetId = targetId;
        }
    }

    private static final class CapturingEligiblePort implements FindEligibleDurableMemoriesPort {
        private AgentOwnerId requestedOwnerId;
        private List<DurableMemory> eligible = List.of();

        @Override
        public List<DurableMemory> findEligible(AgentOwnerId ownerId) {
            requestedOwnerId = ownerId;
            return eligible;
        }
    }

    private static final class CapturingPurgePort implements PurgeDurableMemoriesPort {
        private LocalDateTime boundary;

        @Override
        public void purgeExpiredAndDeletedBefore(LocalDateTime retentionBoundary) {
            boundary = retentionBoundary;
        }
    }
}
