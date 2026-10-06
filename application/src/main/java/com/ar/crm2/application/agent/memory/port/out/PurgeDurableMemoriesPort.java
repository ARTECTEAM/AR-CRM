package com.ar.crm2.application.agent.memory.port.out;

import java.time.LocalDateTime;

public interface PurgeDurableMemoriesPort {
    void purgeExpiredAndDeletedBefore(LocalDateTime retentionBoundary);
}
