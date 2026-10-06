package com.ar.crm2.application.agent.memory.command;

import com.ar.crm2.application.agent.command.ApplicationAssert;
import com.ar.crm2.model.agent.policy.MemorySafetyContext;

import java.util.UUID;

public record ReplaceDurableMemoryCommand(String actorSubject, UUID memoryId, String content, MemorySafetyContext safetyContext) {
    public ReplaceDurableMemoryCommand {
        actorSubject = ApplicationAssert.requiredTrimmed(actorSubject, "actorSubject");
        memoryId = ApplicationAssert.required(memoryId, "memoryId");
        content = ApplicationAssert.requiredTrimmed(content, "content");
        safetyContext = ApplicationAssert.required(safetyContext, "safetyContext");
    }
}
