package com.ar.crm2.model.agent.vo;

import com.ar.crm2.shared.DomainAssert;

import java.util.UUID;

public record MemoryId(UUID value) {

    public MemoryId {
        DomainAssert.notNull(value, "memoryId");
    }
    public static MemoryId create() {
        return new MemoryId(UUID.randomUUID());
    }
    public static MemoryId from(UUID value) {
        return new MemoryId(value);
    }
}
