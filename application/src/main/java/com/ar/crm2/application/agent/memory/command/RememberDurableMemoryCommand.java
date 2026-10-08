package com.ar.crm2.application.agent.memory.command;

import com.ar.crm2.application.shared.ApplicationAssert;
import com.ar.crm2.model.agent.policy.MemorySafetyContext;

public record RememberDurableMemoryCommand(String actorSubject, String content, MemorySafetyContext safetyContext) {
    public RememberDurableMemoryCommand {
        actorSubject = ApplicationAssert.requiredTrimmed(actorSubject, "actorSubject");
        content = ApplicationAssert.requiredTrimmed(content, "content");
        safetyContext = ApplicationAssert.required(safetyContext, "safetyContext");
    }
}
