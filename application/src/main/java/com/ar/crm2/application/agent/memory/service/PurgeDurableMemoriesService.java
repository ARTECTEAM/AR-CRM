package com.ar.crm2.application.agent.memory.service;

import com.ar.crm2.application.agent.memory.command.PurgeDurableMemoriesCommand;
import com.ar.crm2.application.agent.memory.port.in.PurgeDurableMemoriesUseCase;
import com.ar.crm2.application.agent.memory.port.out.PurgeDurableMemoriesPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PurgeDurableMemoriesService implements PurgeDurableMemoriesUseCase {
    private final PurgeDurableMemoriesPort purgePort;

    @Override
    public void purge(PurgeDurableMemoriesCommand command) {
        purgePort.purgeExpiredAndDeletedBefore(command.retentionBoundary());
    }
}
