package com.ar.crm2.application.agent.memory.command;

import com.ar.crm2.application.shared.ApplicationAssert;

import java.time.LocalDateTime;

public record PurgeDurableMemoriesCommand(LocalDateTime retentionBoundary) {
    public PurgeDurableMemoriesCommand {
        retentionBoundary = ApplicationAssert.required(retentionBoundary, "retentionBoundary");
    }
}
