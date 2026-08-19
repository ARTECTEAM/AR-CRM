package com.ar.crm2.adapter.out.persistence.agent.tool;

import com.ar.crm2.exception.InvariantViolationException;
import com.ar.crm2.model.agent.tool.entity.AgentToolAction;
import com.ar.crm2.model.agent.tool.enums.AgentToolActionStatus;
import com.ar.crm2.model.agent.tool.vo.AgentToolName;
import com.ar.crm2.model.agent.tool.vo.AgentToolResource;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:agent-tool-action;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AgentToolActionPersistenceAdapterTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2025, 1, 2, 3, 4, 5);
    private static final LocalDateTime COMPLETION_AT = LocalDateTime.of(2025, 1, 3, 4, 5, 6, 789_000_000);
    private static final Clock FIXED_CLOCK = Clock.fixed(COMPLETION_AT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Autowired
    private AgentToolActionRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private AgentToolActionPersistenceAdapter adapter;

    @BeforeEach
    void createAdapterWithTrustedClock() {
        adapter = new AgentToolActionPersistenceAdapter(repository, transactionManager, FIXED_CLOCK);
    }

    @AfterEach
    void clearPersistenceState() {
        repository.deleteAll();
    }

    @Test
    void roundTripsEveryFieldAndKeepsLookupOwnerScoped() {
        AgentOwnerId owner = AgentOwnerId.from("owner-a");
        TurnId turn = turn("11111111-1111-1111-1111-111111111111");
        AgentToolAction pending = pending(owner, turn, AgentToolName.EDIT_TRATO,
                "{\"id\":\"42\",\"responsableId\":\"77\",\"nombre\":\"Renamed Deal\"}", CREATED_AT);

        adapter.save(pending);
        AgentToolAction completed = adapter.markCompleted(owner, pending.getId(),
                new AgentToolResource("trato", "42"));
        AgentToolAction found = adapter.findByOwnerAndId(owner, pending.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(pending.getId());
        assertThat(found.getOwnerId()).isEqualTo(owner);
        assertThat(found.getTurnId()).isEqualTo(turn);
        assertThat(found.getToolName()).isEqualTo(AgentToolName.EDIT_TRATO);
        assertThat(found.getCanonicalArguments()).isEqualTo(pending.getCanonicalArguments());
        assertThat(found.getStatus()).isEqualTo(AgentToolActionStatus.COMPLETED);
        assertThat(found.getResource()).isEqualTo(new AgentToolResource("trato", "42"));
        assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(completed.getCompletedAt()).isEqualTo(COMPLETION_AT);
        assertThat(found.getCompletedAt()).isEqualTo(COMPLETION_AT);
        assertThat(adapter.findByOwnerAndId(AgentOwnerId.from("owner-b"), pending.getId())).isEmpty();
    }

    @Test
    void assignedIdIsPersistedAsNewAndSequentialReplayCannotRegressCompletion() {
        AgentOwnerId owner = AgentOwnerId.from("owner-a");
        AgentToolAction pending = pending(owner, turn("22222222-2222-2222-2222-222222222222"),
                AgentToolName.CREATE_CONTACT, "{\"companyId\":\"7\"}", CREATED_AT);

        AgentToolActionEntity transientEntity = AgentToolActionPersistenceMapper.toEntity(pending);
        assertThat(transientEntity.getId()).isEqualTo(pending.getId().value());
        assertThat(transientEntity.getVersion()).isNull();

        AgentToolAction claimed = adapter.save(pending);
        assertThat(claimed).isEqualTo(pending);
        assertThat(repository.findById(pending.getId().value()).orElseThrow().getVersion()).isZero();

        AgentToolAction firstCompletion = adapter.markCompleted(owner, pending.getId(),
                new AgentToolResource("contacto", "first"));
        AgentToolAction replayedClaim = adapter.save(pending);
        AgentToolAction completedReplay = adapter.save(firstCompletion);
        AgentToolAction replayedCompletion = adapter.markCompleted(owner, pending.getId(),
                new AgentToolResource("contacto", "ignored"));

        assertThat(replayedClaim.getStatus()).isEqualTo(AgentToolActionStatus.COMPLETED);
        assertThat(replayedClaim.getResource()).isEqualTo(firstCompletion.getResource());
        assertThat(completedReplay).isEqualTo(firstCompletion);
        assertThat(replayedCompletion.getResource()).isEqualTo(firstCompletion.getResource());
        assertThat(replayedCompletion.getCompletedAt()).isEqualTo(firstCompletion.getCompletedAt());
        assertThat(repository.count()).isOne();
    }

    @Test
    void rejectsAnInitiallyCompletedAggregateAndLeavesTheDatabaseUnchanged() {
        AgentOwnerId owner = AgentOwnerId.from("owner-a");
        AgentToolAction pending = pending(owner, turn("66666666-6666-6666-6666-666666666666"),
                AgentToolName.CREATE_CONTACT, "{\"companyId\":\"already-complete\"}", CREATED_AT);
        AgentToolAction completed = pending.completeWith(
                new AgentToolResource("contacto", "already-complete"), COMPLETION_AT);

        assertThatThrownBy(() -> adapter.save(completed))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only PENDING agent tool actions can be claimed")
                .hasNoCause();

        assertThat(repository.count()).isZero();
        assertThat(adapter.findByOwnerAndId(owner, pending.getId())).isEmpty();
    }

    @Test
    void completionByAnotherOwnerFailsWithoutChangingTheCanonicalPendingRow() {
        AgentOwnerId ownerA = AgentOwnerId.from("owner-a");
        AgentOwnerId ownerB = AgentOwnerId.from("owner-b");
        AgentToolAction pending = pending(ownerA, turn("77777777-7777-7777-7777-777777777777"),
                AgentToolName.CREATE_CONTACT, "{\"companyId\":\"owner-a-only\"}", CREATED_AT);
        adapter.save(pending);

        assertThatThrownBy(() -> adapter.markCompleted(ownerB, pending.getId(),
                new AgentToolResource("contacto", "forbidden")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Agent tool action was not found for the owner")
                .hasNoCause();

        AgentToolAction unchanged = adapter.findByOwnerAndId(ownerA, pending.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(AgentToolActionStatus.PENDING);
        assertThat(unchanged.getResource()).isNull();
        assertThat(unchanged.getCompletedAt()).isNull();
    }

    @Test
    void completionDelegatesChronologyToTheDomainTransition() {
        AgentOwnerId owner = AgentOwnerId.from("owner-a");
        AgentToolAction futurePending = pending(owner, turn("33333333-3333-3333-3333-333333333333"),
                AgentToolName.CREATE_CONTACT, "{\"companyId\":\"future\"}",
                LocalDateTime.of(2099, 1, 1, 0, 0));
        adapter.save(futurePending);

        assertThatThrownBy(() -> adapter.markCompleted(owner, futurePending.getId(),
                new AgentToolResource("contacto", "future")))
                .isInstanceOf(InvariantViolationException.class);

        AgentToolAction unchanged = adapter.findByOwnerAndId(owner, futurePending.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(AgentToolActionStatus.PENDING);
        assertThat(unchanged.getResource()).isNull();
        assertThat(unchanged.getCompletedAt()).isNull();
    }

    @Test
    void concurrentClaimsUseSeparateThreadsAndConvergeOnOneCanonicalRow() throws Exception {
        AgentOwnerId owner = AgentOwnerId.from("owner-a");
        TurnId turn = turn("44444444-4444-4444-4444-444444444444");
        AgentToolAction firstCandidate = pending(owner, turn, AgentToolName.CREATE_CONTACT,
                "{\"companyId\":\"race\"}", CREATED_AT);
        AgentToolAction secondCandidate = pending(owner, turn, AgentToolName.CREATE_CONTACT,
                "{\"companyId\":\"race\"}", CREATED_AT.plusSeconds(1));

        List<AgentToolAction> results = race(
                () -> adapter.save(firstCandidate),
                () -> adapter.save(secondCandidate));
        AgentToolAction canonical = adapter.findByOwnerAndId(owner, firstCandidate.getId()).orElseThrow();

        assertThat(repository.count()).isOne();
        assertThat(results).allSatisfy(result -> {
            assertThat(result.getId()).isEqualTo(canonical.getId());
            assertThat(result.getCreatedAt()).isEqualTo(canonical.getCreatedAt());
            assertThat(result.getStatus()).isEqualTo(AgentToolActionStatus.PENDING);
        });
        assertThat(canonical.getCreatedAt()).isIn(CREATED_AT, CREATED_AT.plusSeconds(1));
    }

    @Test
    void completionWaitsForARealHeldLockThenConvergesWithoutReplacement() throws Exception {
        AgentOwnerId owner = AgentOwnerId.from("owner-a");
        AgentToolAction pending = pending(owner, turn("55555555-5555-5555-5555-555555555555"),
                AgentToolName.CREATE_CONTACT, "{\"companyId\":\"held-lock\"}", CREATED_AT);
        adapter.save(pending);
        AgentToolResource firstCandidate = new AgentToolResource("contacto", "held-first");
        AgentToolResource replayCandidate = new AgentToolResource("contacto", "must-be-ignored");
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        CountDownLatch contenderStarted = new CountDownLatch(1);
        CountDownLatch completionReturned = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> holder = executor.submit(() -> holdLock(
                    owner, pending, lockAcquired, releaseLock));
            assertThat(lockAcquired.await(5, TimeUnit.SECONDS)).isTrue();

            Future<AgentToolAction> contender = executor.submit(() -> {
                contenderStarted.countDown();
                try {
                    return adapter.markCompleted(owner, pending.getId(), firstCandidate);
                } finally {
                    completionReturned.countDown();
                }
            });
            assertThat(contenderStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(completionReturned.await(750, TimeUnit.MILLISECONDS))
                    .as("the contender must remain blocked while the real row lock is held")
                    .isFalse();

            releaseLock.countDown();
            AgentToolAction first = contender.get(15, TimeUnit.SECONDS);
            holder.get(15, TimeUnit.SECONDS);
            AgentToolAction replay = adapter.markCompleted(owner, pending.getId(), replayCandidate);

            assertThat(first.getResource()).isEqualTo(firstCandidate);
            assertThat(first.getCompletedAt()).isEqualTo(COMPLETION_AT);
            assertThat(replay.getResource()).isEqualTo(firstCandidate);
            assertThat(replay.getCompletedAt()).isEqualTo(COMPLETION_AT);
            assertThat(repository.count()).isOne();
        } finally {
            releaseLock.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void persistsCanonicalToolNameAndReconstitutesItFromTheDatabase() {
        AgentOwnerId owner = AgentOwnerId.from("owner-a");
        AgentToolAction pending = pending(owner, turn("88888888-8888-8888-8888-888888888888"),
                AgentToolName.EDIT_TRATO, "{\"id\":\"42\",\"nombre\":\"Renamed Deal\"}", CREATED_AT);
        adapter.save(pending);

        AgentToolActionEntity stored = repository.findById(pending.getId().value()).orElseThrow();
        AgentToolAction roundTripped = adapter.findByOwnerAndId(owner, pending.getId()).orElseThrow();

        assertThat(stored.getToolName()).isEqualTo(AgentToolName.EDIT_TRATO.storageName());
        assertThat(roundTripped.getToolName()).isEqualTo(AgentToolName.EDIT_TRATO);
    }

    @Test
    void sanitizesUnrecoverablePersistenceFailuresAtTheAdapterBoundary() {
        AgentToolActionRepository failingRepository = mock(AgentToolActionRepository.class);
        when(failingRepository.findByOwnerIdAndId(anyString(), anyString()))
                .thenThrow(new DataAccessResourceFailureException(
                        "SQL owner-secret arguments-secret provider-stack"));
        AgentToolActionPersistenceAdapter failingAdapter = new AgentToolActionPersistenceAdapter(
                failingRepository, transactionManager, FIXED_CLOCK);

        assertThatThrownBy(() -> failingAdapter.findByOwnerAndId(
                AgentOwnerId.from("owner-secret"),
                com.ar.crm2.model.agent.tool.vo.AgentToolActionId.derive(
                        AgentOwnerId.from("owner-secret"),
                        turn("99999999-9999-9999-9999-999999999999"),
                        AgentToolName.CREATE_CONTACT,
                        "{\"secret\":\"payload\"}")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Agent tool action persistence failed")
                .hasNoCause()
                .hasMessageNotContaining("owner-secret")
                .hasMessageNotContaining("provider-stack")
                .hasMessageNotContaining("arguments-secret");
    }

    private void holdLock(AgentOwnerId owner, AgentToolAction action,
                          CountDownLatch lockAcquired, CountDownLatch releaseLock) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.execute(ignored -> {
            repository.findByOwnerIdAndIdForUpdate(owner.value(), action.getId().value())
                    .orElseThrow();
            lockAcquired.countDown();
            try {
                if (!releaseLock.await(15, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("lock release timed out");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("lock holder interrupted");
            }
            return null;
        });
    }

    private AgentToolAction pending(AgentOwnerId owner, TurnId turn, AgentToolName tool,
                                    String arguments, LocalDateTime createdAt) {
        Clock clock = Clock.fixed(createdAt.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return AgentToolAction.createPending(owner, turn, tool, arguments, clock);
    }

    private TurnId turn(String value) {
        return TurnId.from(UUID.fromString(value));
    }

    private <T> List<T> race(Callable<T> first, Callable<T> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<T> firstResult = executor.submit(awaitStart(ready, start, first));
            Future<T> secondResult = executor.submit(awaitStart(ready, start, second));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(firstResult.get(15, TimeUnit.SECONDS), secondResult.get(15, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private <T> Callable<T> awaitStart(CountDownLatch ready, CountDownLatch start, Callable<T> task) {
        return () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return task.call();
        };
    }
}
