package com.ar.crm2.application.agent.memory.command;

import com.ar.crm2.application.shared.ApplicationAssert;

import java.util.UUID;

public record DeleteDurableMemoryCommand(String actorSubject, UUID memoryId) {
    public DeleteDurableMemoryCommand {
        actorSubject = ApplicationAssert.requiredTrimmed(actorSubject, "actorSubject");
        memoryId = ApplicationAssert.required(memoryId, "memoryId");
    }
}
