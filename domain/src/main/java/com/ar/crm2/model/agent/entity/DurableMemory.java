package com.ar.crm2.model.agent.entity;

import com.ar.crm2.exception.DurableMemoryLifecycleException;
import com.ar.crm2.exception.InvariantViolationException;
import com.ar.crm2.model.agent.enums.DurableMemoryStatus;
import com.ar.crm2.model.agent.policy.MemorySafetyContext;
import com.ar.crm2.model.agent.policy.MemorySafetyPolicy;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.MemoryId;
import com.ar.crm2.shared.DomainAssert;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Immutable owner-bound durable memory with an explicit lifecycle.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"ownerId", "content"})
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DurableMemory {

    @EqualsAndHashCode.Include
    private final MemoryId id;
    private final AgentOwnerId ownerId;
    private final String content;
    private final DurableMemoryStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime expiresAt;
    private final LocalDateTime supersededAt;
    private final MemoryId supersededById;
    private final LocalDateTime deletedAt;

    public static DurableMemory create(AgentOwnerId ownerId, String content, MemorySafetyContext safetyContext) {
        LocalDateTime now = LocalDateTime.now();
        DomainAssert.notNull(ownerId, "agentOwnerId");
        DomainAssert.notBlank(content, "memoryContent");
        MemorySafetyPolicy.requireEligible(safetyContext);
        return new DurableMemory(MemoryId.create(), ownerId, content.trim(), DurableMemoryStatus.ACTIVE,
                now, now, null, null, null, null);
    }

    public static DurableMemory reconstitute(MemoryId id, AgentOwnerId ownerId, String content,
                                             DurableMemoryStatus status, LocalDateTime createdAt, LocalDateTime updatedAt,
                                             LocalDateTime expiresAt, LocalDateTime supersededAt,
                                             MemoryId supersededById, LocalDateTime deletedAt) {
        DomainAssert.notNull(id, "memoryId");
        DomainAssert.notNull(ownerId, "agentOwnerId");
        DomainAssert.notBlank(content, "memoryContent");
        DomainAssert.notNull(status, "durableMemoryStatus");
        DomainAssert.notNull(createdAt, "createdAt");
        DomainAssert.notNull(updatedAt, "updatedAt");
        validateLifecycle(id, status, supersededAt, supersededById, deletedAt);
        return new DurableMemory(id, ownerId, content.trim(), status, createdAt, updatedAt, expiresAt,
                supersededAt, supersededById, deletedAt);
    }

    public DurableMemory supersedeWith(DurableMemory successor, AgentOwnerId ownerId, MemoryId explicitTargetId) {
        DomainAssert.notNull(successor, "successorMemory");
        requireActiveOwnerAndTarget(ownerId, explicitTargetId);
        if (!ownerId.equals(successor.ownerId) || id.equals(successor.id)) {
            throw DurableMemoryLifecycleException.ownerOrTargetMismatch();
        }
        LocalDateTime now = LocalDateTime.now();
        if (successor.status != DurableMemoryStatus.ACTIVE || !successor.isEligibleAt(now)) {
            throw DurableMemoryLifecycleException.invalidSuccessor();
        }
        return new DurableMemory(id, this.ownerId, content, DurableMemoryStatus.SUPERSEDED, createdAt, now, expiresAt,
                now, successor.id, null);
    }

    public DurableMemory delete(AgentOwnerId ownerId, MemoryId explicitTargetId) {
        requireActiveOwnerAndTarget(ownerId, explicitTargetId);
        LocalDateTime now = LocalDateTime.now();
        return new DurableMemory(id, this.ownerId, content, DurableMemoryStatus.DELETED, createdAt, now, expiresAt,
                null, null, now);
    }

    public boolean isEligibleAt(LocalDateTime now) {
        DomainAssert.notNull(now, "currentTime");
        return status == DurableMemoryStatus.ACTIVE && deletedAt == null
                && (expiresAt == null || expiresAt.isAfter(now));
    }

    private void requireActiveOwnerAndTarget(AgentOwnerId ownerId, MemoryId explicitTargetId) {
        DomainAssert.notNull(ownerId, "agentOwnerId");
        DomainAssert.notNull(explicitTargetId, "explicitMemoryTargetId");
        if (status != DurableMemoryStatus.ACTIVE) {
            throw DurableMemoryLifecycleException.invalidTransition();
        }
        if (!this.ownerId.equals(ownerId) || !id.equals(explicitTargetId)) {
            throw DurableMemoryLifecycleException.ownerOrTargetMismatch();
        }
    }

    private static void validateLifecycle(MemoryId id, DurableMemoryStatus status, LocalDateTime supersededAt,
                                           MemoryId supersededById, LocalDateTime deletedAt) {
        boolean superseded = supersededAt != null && supersededById != null && deletedAt == null;
        boolean deleted = deletedAt != null && supersededAt == null && supersededById == null;
        boolean active = supersededAt == null && supersededById == null && deletedAt == null;
        if ((status == DurableMemoryStatus.SUPERSEDED && !superseded)
                || (status == DurableMemoryStatus.DELETED && !deleted)
                || (status == DurableMemoryStatus.ACTIVE && !active)
                || (status == DurableMemoryStatus.SUPERSEDED && id.equals(supersededById))) {
            throw new InvariantViolationException("La combinación de estado de memoria durable es inválida.");
        }
    }
}
