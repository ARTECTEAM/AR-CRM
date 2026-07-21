package com.ar.crm2.application.agent.memory.command;

import com.ar.crm2.model.agent.policy.MemorySafetyContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurableMemoryCommandTest {

    @Test
    void normalizesRequiredTextInputs() {
        RememberDurableMemoryCommand remember = new RememberDurableMemoryCommand(
                " owner-a ", " Use Spanish ", MemorySafetyContext.explicitSafe());
        ReplaceDurableMemoryCommand replace = new ReplaceDurableMemoryCommand(
                " owner-a ", UUID.randomUUID(), " Use English ", MemorySafetyContext.explicitSafe());
        RecallDurableMemoriesCommand recall = new RecallDurableMemoriesCommand(" owner-a ");

        assertEquals("owner-a", remember.actorSubject());
        assertEquals("Use Spanish", remember.content());
        assertEquals("owner-a", replace.actorSubject());
        assertEquals("Use English", replace.content());
        assertEquals("owner-a", recall.actorSubject());
    }

    @Test
    void rejectsMissingRequiredCommandArguments() {
        assertRequired("actorSubject", () -> new RememberDurableMemoryCommand(
                " ", "content", MemorySafetyContext.explicitSafe()));
        assertRequired("content", () -> new RememberDurableMemoryCommand(
                "owner-a", " ", MemorySafetyContext.explicitSafe()));
        assertRequired("safetyContext", () -> new RememberDurableMemoryCommand("owner-a", "content", null));

        assertRequired("actorSubject", () -> new ReplaceDurableMemoryCommand(
                null, UUID.randomUUID(), "content", MemorySafetyContext.explicitSafe()));
        assertRequired("memoryId", () -> new ReplaceDurableMemoryCommand(
                "owner-a", null, "content", MemorySafetyContext.explicitSafe()));
        assertRequired("content", () -> new ReplaceDurableMemoryCommand(
                "owner-a", UUID.randomUUID(), " ", MemorySafetyContext.explicitSafe()));
        assertRequired("safetyContext", () -> new ReplaceDurableMemoryCommand(
                "owner-a", UUID.randomUUID(), "content", null));

        assertRequired("actorSubject", () -> new DeleteDurableMemoryCommand(" ", UUID.randomUUID()));
        assertRequired("memoryId", () -> new DeleteDurableMemoryCommand("owner-a", null));

        assertRequired("actorSubject", () -> new RecallDurableMemoriesCommand(null));
        assertRequired("retentionBoundary", () -> new PurgeDurableMemoriesCommand(null));
    }

    @Test
    void retainsValidStructuredArguments() {
        UUID memoryId = UUID.randomUUID();
        LocalDateTime retentionBoundary = LocalDateTime.of(2026, 7, 20, 10, 0);
        MemorySafetyContext safetyContext = MemorySafetyContext.explicitSafe();

        assertEquals(memoryId, new DeleteDurableMemoryCommand("owner-a", memoryId).memoryId());
        assertEquals(safetyContext, new ReplaceDurableMemoryCommand(
                "owner-a", memoryId, "content", safetyContext).safetyContext());
        assertEquals(retentionBoundary, new PurgeDurableMemoriesCommand(retentionBoundary).retentionBoundary());
    }

    private void assertRequired(String field, Runnable construction) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, construction::run);
        assertEquals(field + " is required", exception.getMessage());
    }
}
