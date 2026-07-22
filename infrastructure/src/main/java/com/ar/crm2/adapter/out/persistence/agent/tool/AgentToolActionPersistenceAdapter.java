package com.ar.crm2.adapter.out.persistence.agent.tool;

import com.ar.crm2.application.agent.tool.port.out.FindAgentToolActionByIdPort;
import com.ar.crm2.application.agent.tool.port.out.MarkAgentToolActionCompletedPort;
import com.ar.crm2.application.agent.tool.port.out.SaveAgentToolActionPort;
import com.ar.crm2.model.agent.tool.entity.AgentToolAction;
import com.ar.crm2.model.agent.tool.enums.AgentToolActionStatus;
import com.ar.crm2.model.agent.tool.vo.AgentToolActionId;
import com.ar.crm2.model.agent.tool.vo.AgentToolResource;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * Short-transaction action ledger adapter. Claims use a deterministic primary
 * key plus insert-and-recover; completion serializes contenders with a
 * pessimistic row lock and applies the Domain transition before persistence.
 */
@RequiredArgsConstructor
public class AgentToolActionPersistenceAdapter implements SaveAgentToolActionPort,
        FindAgentToolActionByIdPort, MarkAgentToolActionCompletedPort {

    private final AgentToolActionRepository repository;
    private final PlatformTransactionManager transactionManager;
    private final Clock clock;

    @Override
    public AgentToolAction save(AgentToolAction action) {
        Objects.requireNonNull(action, "action");
        try {
            return inNewTransaction(ignored -> repository
                    .findByOwnerIdAndId(action.getOwnerId().value(), action.getId().value())
                    .map(AgentToolActionPersistenceMapper::toDomain)
                    .orElseGet(() -> {
                        requirePendingClaim(action);
                        return AgentToolActionPersistenceMapper.toDomain(
                                repository.saveAndFlush(AgentToolActionPersistenceMapper.toEntity(action)));
                    }));
        } catch (DataIntegrityViolationException ignoredDuplicate) {
            return recoverCanonicalClaim(action);
        } catch (DataAccessException | TransactionException failure) {
            throw persistenceFailure();
        }
    }

    @Override
    public Optional<AgentToolAction> findByOwnerAndId(AgentOwnerId ownerId, AgentToolActionId actionId) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(actionId, "actionId");
        try {
            return inNewTransaction(ignored -> repository
                    .findByOwnerIdAndId(ownerId.value(), actionId.value())
                    .map(AgentToolActionPersistenceMapper::toDomain));
        } catch (DataAccessException | TransactionException failure) {
            throw persistenceFailure();
        }
    }

    @Override
    public AgentToolAction markCompleted(AgentOwnerId ownerId, AgentToolActionId actionId,
                                         AgentToolResource resource) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(resource, "resource");
        try {
            return inNewTransaction(ignored -> {
                AgentToolActionEntity locked = repository
                        .findByOwnerIdAndIdForUpdate(ownerId.value(), actionId.value())
                        .orElseThrow(() -> new IllegalStateException(
                                "Agent tool action was not found for the owner"));
                AgentToolAction current = AgentToolActionPersistenceMapper.toDomain(locked);
                AgentToolAction completed = current.completeWith(resource, completionTime());
                if (completed == current) {
                    return current;
                }
                AgentToolActionEntity saved = repository.saveAndFlush(
                        AgentToolActionPersistenceMapper.toEntity(completed, locked.getVersion()));
                return AgentToolActionPersistenceMapper.toDomain(saved);
            });
        } catch (DataAccessException | TransactionException failure) {
            throw persistenceFailure();
        }
    }

    private AgentToolAction recoverCanonicalClaim(AgentToolAction action) {
        try {
            return inNewTransaction(ignored -> repository
                    .findByOwnerIdAndId(action.getOwnerId().value(), action.getId().value())
                    .map(AgentToolActionPersistenceMapper::toDomain)
                    .orElseThrow(this::persistenceFailure));
        } catch (DataAccessException | TransactionException failure) {
            throw persistenceFailure();
        }
    }

    private void requirePendingClaim(AgentToolAction action) {
        if (action.getStatus() != AgentToolActionStatus.PENDING
                || action.getResource() != null
                || action.getCompletedAt() != null) {
            throw new IllegalArgumentException("Only PENDING agent tool actions can be claimed");
        }
    }

    private LocalDateTime completionTime() {
        return LocalDateTime.now(Objects.requireNonNull(clock, "clock").withZone(ZoneOffset.UTC))
                .truncatedTo(ChronoUnit.MILLIS);
    }

    private <T> T inNewTransaction(TransactionCallback<T> callback) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return Objects.requireNonNull(template.execute(callback));
    }

    private IllegalStateException persistenceFailure() {
        return new IllegalStateException("Agent tool action persistence failed");
    }
}
