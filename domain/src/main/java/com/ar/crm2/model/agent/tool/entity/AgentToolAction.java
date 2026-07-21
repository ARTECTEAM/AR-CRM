package com.ar.crm2.model.agent.tool.entity;

import com.ar.crm2.exception.InvariantViolationException;
import com.ar.crm2.model.agent.tool.enums.AgentToolActionStatus;
import com.ar.crm2.model.agent.tool.vo.AgentToolActionId;
import com.ar.crm2.model.agent.tool.vo.AgentToolName;
import com.ar.crm2.model.agent.tool.vo.AgentToolResource;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.shared.DomainAssert;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Owner-scoped, server-derived record of a single agent tool invocation.
 * The identity is the SHA-256 of {@code (owner, turn, tool, canonical
 * arguments)} so a retry with the same trusted inputs converges on the
 * same canonical row. The lifecycle is closed: the action is born
 * {@code PENDING} and either stays absent or transitions to
 * {@code COMPLETED} with a fixed canonical resource. Once {@code COMPLETED}
 * the canonical resource and timestamp are immutable.
 *
 * <p>The generated string representation is deliberately allowlisted to the
 * non-sensitive tool name and lifecycle status. Identity, owner, turn,
 * canonical arguments, resource details, and timestamps are never emitted.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentToolAction {

    @EqualsAndHashCode.Include
    private final AgentToolActionId id;
    private final AgentOwnerId ownerId;
    private final TurnId turnId;
    @ToString.Include
    private final AgentToolName toolName;
    private final String canonicalArguments;
    @ToString.Include
    private final AgentToolActionStatus status;
    private final AgentToolResource resource;
    private final LocalDateTime createdAt;
    private final LocalDateTime completedAt;

    /**
     * Create a new PENDING action. The {@code createdAt} timestamp is
     * generated inside the factory from a trusted clock — callers cannot
     * supply the timestamp directly — and the identity is derived from the
     * four trusted inputs so retries converge at the persistence boundary.
     */
    public static AgentToolAction createPending(
            AgentOwnerId ownerId,
            TurnId turnId,
            AgentToolName toolName,
            String canonicalArguments,
            Clock clock
    ) {
        DomainAssert.notNull(ownerId, "ownerId");
        DomainAssert.notNull(turnId, "turnId");
        DomainAssert.notNull(toolName, "toolName");
        DomainAssert.notBlank(canonicalArguments, "canonicalArguments");
        DomainAssert.notNull(clock, "clock");
        String normalizedCanonicalArguments = canonicalArguments.trim();
        AgentToolActionId id = AgentToolActionId.derive(
                ownerId, turnId, toolName, normalizedCanonicalArguments);
        return new AgentToolAction(id, ownerId, turnId, toolName, normalizedCanonicalArguments,
                AgentToolActionStatus.PENDING, null, LocalDateTime.now(clock), null);
    }

    /**
     * Reconstitute a persisted action. This factory is reserved for the
     * persistence mapper: it preserves the stored state without
     * re-asserting lifecycle transitions, but it does enforce the same
     * structural invariants — every identity, owner, turn, tool, canonical
     * arguments, status, and createdAt is required; a {@code COMPLETED}
     * row must carry a non-null resource and completedAt; a {@code PENDING}
     * row must carry both as {@code null}.
     */
    public static AgentToolAction reconstitute(
            AgentToolActionId id,
            AgentOwnerId ownerId,
            TurnId turnId,
            AgentToolName toolName,
            String canonicalArguments,
            AgentToolActionStatus status,
            AgentToolResource resource,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {
        DomainAssert.notNull(id, "id");
        DomainAssert.notNull(ownerId, "ownerId");
        DomainAssert.notNull(turnId, "turnId");
        DomainAssert.notNull(toolName, "toolName");
        DomainAssert.notBlank(canonicalArguments, "canonicalArguments");
        DomainAssert.notNull(status, "status");
        DomainAssert.notNull(createdAt, "createdAt");
        String normalizedCanonicalArguments = canonicalArguments.trim();
        AgentToolActionId expectedId = AgentToolActionId.derive(
                ownerId, turnId, toolName, normalizedCanonicalArguments);
        DomainAssert.sameAs(id, expectedId, "id");
        if (status == AgentToolActionStatus.COMPLETED) {
            DomainAssert.notNull(resource, "resource");
            DomainAssert.notNull(completedAt, "completedAt");
            requireValidChronology(createdAt, completedAt);
        } else if (status == AgentToolActionStatus.PENDING) {
            if (resource != null) {
                throw new InvariantViolationException(
                        "Una acción en estado PENDING no debe portar un recurso canónico.");
            }
            if (completedAt != null) {
                throw new InvariantViolationException(
                        "Una acción en estado PENDING no debe portar fecha de finalización.");
            }
        }
        return new AgentToolAction(id, ownerId, turnId, toolName, normalizedCanonicalArguments, status,
                resource, createdAt, completedAt);
    }

    /**
     * Return a COMPLETED action carrying the canonical resource and the
     * completion timestamp.
     *
     * <p>Calling this on a {@code PENDING} action is the only path that
     * introduces a new resource and timestamp. Calling it on an already
     * {@code COMPLETED} action is idempotent at the Domain level: the
     * first canonical resource and first completedAt are preserved, the
     * replayed resource/completedAt are ignored, and the same instance is
     * returned. The {@code #equals}/{@code #hashCode} identity, owner,
     * turn, tool, canonical arguments, and createdAt are never overwritten
     * — replacing them would be a silent convergence violation.
     * Persistence-layer replay convergence on a deterministic id is
     * enforced by the infrastructure adapter and is independent of this
     * idempotency.
     */
    public AgentToolAction completeWith(AgentToolResource resource, LocalDateTime completedAt) {
        DomainAssert.notNull(resource, "resource");
        DomainAssert.notNull(completedAt, "completedAt");
        if (status == AgentToolActionStatus.COMPLETED) {
            return this;
        }
        requireValidChronology(createdAt, completedAt);
        return new AgentToolAction(id, ownerId, turnId, toolName, canonicalArguments,
                AgentToolActionStatus.COMPLETED, resource, createdAt, completedAt);
    }

    private static void requireValidChronology(LocalDateTime createdAt, LocalDateTime completedAt) {
        if (completedAt.isBefore(createdAt)) {
            throw new InvariantViolationException(
                    "La fecha de finalización no puede ser anterior a la fecha de creación.");
        }
    }
}
