package com.ar.crm2.application.agent.memory.command;

import com.ar.crm2.application.agent.command.ApplicationAssert;

public record RecallDurableMemoriesCommand(String actorSubject) {
    public RecallDurableMemoriesCommand {
        actorSubject = ApplicationAssert.requiredTrimmed(actorSubject, "actorSubject");
    }
}
