package com.ar.crm2.adapter.out.persistence.agent;

import com.ar.crm2.adapter.out.persistence.agent.entity.AgentVisibleHistoryEntity;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentConversationRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentTurnRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentTurnRequestRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentVisibleHistoryRepository;
import com.ar.crm2.application.agent.turn.exception.IdempotencyKeyReusedException;
import com.ar.crm2.model.agent.entity.AgentTurn;
import com.ar.crm2.model.agent.entity.Conversation;
import com.ar.crm2.model.agent.enums.TurnState;
import com.ar.crm2.model.agent.vo.AcceptedUserTurn;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({AgentTurnAdapter.class, AgentTurnAdapterTest.RepositoryLookupBarrierConfiguration.class})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:agent-turn-creation;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AgentTurnAdapterTest {

    @Autowired
    private AgentTurnAdapter adapter;

    @Autowired
    private AgentConversationRepository conversationRepository;

    @Autowired
    private AgentTurnRepository turnRepository;

    @Autowired
    private AgentTurnRequestRepository requestRepository;

    @Autowired
    private AgentVisibleHistoryRepository historyRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RepositoryLookupBarrier repositoryLookupBarrier;

    @AfterEach
    void clearPersistenceState() {
        historyRepository.deleteAll();
        requestRepository.deleteAll();
        turnRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    @Test
    void atomicallyCreatesThePreparedTurnRequestAndNormalizedUserHistoryThenConvergesRetries() {
        AcceptedUserTurn first = create("owner-a", "key-1", "fingerprint-1", "Hello Pipely");
        AcceptedUserTurn retried = create("owner-a", "key-1", "fingerprint-1", "ignored retry content");

        assertThat(retried).isEqualTo(first);
        assertThat(conversationRepository.count()).isEqualTo(1);
        assertThat(turnRepository.count()).isEqualTo(1);
        assertThat(requestRepository.count()).isEqualTo(1);
        assertThat(historyRepository.count()).isEqualTo(1);
        AgentVisibleHistoryEntity history = historyRepository.findAll().getFirst();
        assertThat(history.getRole()).isEqualTo("USER");
        assertThat(history.getContent()).isEqualTo("Hello Pipely");
        assertThat(history.getTurn().getId()).isEqualTo(first.turn().getId().value().toString());
        assertThat(first.turn().getState()).isEqualTo(TurnState.PREPARED);
    }

    @Test
    void rejectsDifferentFingerprintWithoutMutatingAndKeepsOwnerKeysIsolated() {
        AcceptedUserTurn ownerA = create("owner-a", "key-1", "fingerprint-1", "prompt-a");

        assertThatThrownBy(() -> create("owner-a", "key-1", "fingerprint-2", "changed prompt"))
                .isInstanceOf(IdempotencyKeyReusedException.class);
        AcceptedUserTurn ownerB = create("owner-b", "key-1", "fingerprint-2", "prompt-b");

        assertThat(ownerB).isNotEqualTo(ownerA);
        assertThat(conversationRepository.count()).isEqualTo(2);
        assertThat(turnRepository.count()).isEqualTo(2);
        assertThat(requestRepository.count()).isEqualTo(2);
        assertThat(historyRepository.count()).isEqualTo(2);
    }

    @Test
    void rollsBackEveryCreationWriteWhenTheVisibleContentCannotBePersisted() {
        assertThatThrownBy(() -> create("owner-a", "key-1", "fingerprint-1", null))
                .isInstanceOf(RuntimeException.class);
        entityManager.clear();

        assertThat(conversationRepository.count()).isZero();
        assertThat(turnRepository.count()).isZero();
        assertThat(requestRepository.count()).isZero();
        assertThat(historyRepository.count()).isZero();
    }

    @Test
    void concurrentRequestsWithTheSameIdempotencyKeyConvergeOnTheCanonicalReceipt() throws Exception {
        create("owner-race", "existing-key", "existing-fingerprint", "existing prompt");
        repositoryLookupBarrier.arm("findByOwnerIdAndIdempotencyKey", "owner-race", "same-key");

        AcceptedUserTurn first;
        AcceptedUserTurn second;
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<AcceptedUserTurn> firstRequest = executor.submit(
                    () -> create("owner-race", "same-key", "same-fingerprint", "same prompt"));
            Future<AcceptedUserTurn> secondRequest = executor.submit(
                    () -> create("owner-race", "same-key", "same-fingerprint", "same prompt"));
            assertThat(repositoryLookupBarrier.awaitBothCalls())
                    .as("both requests read the absent idempotency receipt before either writes")
                    .isTrue();
            first = firstRequest.get(10, TimeUnit.SECONDS);
            second = secondRequest.get(10, TimeUnit.SECONDS);
        }

        assertThat(second).isEqualTo(first);
        assertThat(conversationRepository.count()).isEqualTo(1);
        assertThat(turnRepository.count()).isEqualTo(2);
        assertThat(requestRepository.count()).isEqualTo(2);
        assertThat(historyRepository.count()).isEqualTo(2);
    }

    @Test
    void concurrentFirstRequestsWithDifferentKeysConvergeOnOneOwnerConversation() throws Exception {
        repositoryLookupBarrier.arm("findByOwnerId", "owner-race");

        AcceptedUserTurn first;
        AcceptedUserTurn second;
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<AcceptedUserTurn> firstRequest = executor.submit(
                    () -> create("owner-race", "key-one", "fingerprint-one", "first prompt"));
            Future<AcceptedUserTurn> secondRequest = executor.submit(
                    () -> create("owner-race", "key-two", "fingerprint-two", "second prompt"));
            assertThat(repositoryLookupBarrier.awaitBothCalls())
                    .as("both requests observe that the owner has no conversation before either writes")
                    .isTrue();
            first = firstRequest.get(10, TimeUnit.SECONDS);
            second = secondRequest.get(10, TimeUnit.SECONDS);
        }

        assertThat(second).isNotEqualTo(first);
        assertThat(conversationRepository.count()).isEqualTo(1);
        assertThat(turnRepository.count()).isEqualTo(2);
        assertThat(requestRepository.count()).isEqualTo(2);
        assertThat(historyRepository.findAll())
                .extracting(AgentVisibleHistoryEntity::getContent)
                .containsExactlyInAnyOrder("first prompt", "second prompt");
    }

    @Test
    void persistsLongUserAndAssistantHistoryWithoutTruncation() {
        String userContent = "user-message-".repeat(256);
        String assistantContent = "assistant-message-".repeat(256);
        AcceptedUserTurn accepted = create("owner-long-content", "key-long", "fingerprint-long", userContent);

        assertThat(adapter.completePreparedTurn(
                AgentOwnerId.from("owner-long-content"),
                accepted.turn().getId(),
                accepted.opaqueHandle(),
                assistantContent
        )).isEqualTo(assistantContent);

        assertThat(historyRepository.findAll())
                .extracting(AgentVisibleHistoryEntity::getContent)
                .containsExactlyInAnyOrder(userContent, assistantContent);
    }

    private AcceptedUserTurn create(String owner, String key, String fingerprint, String content) {
        AgentOwnerId ownerId = AgentOwnerId.from(owner);
        Conversation conversation = Conversation.create(ownerId);
        AgentTurn turn = conversation.createTurn(TurnId.create());
        return adapter.createOrGetUserTurn(
                conversation,
                turn,
                ownerId,
                key,
                content,
                fingerprint,
                UUID.randomUUID().toString()
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RepositoryLookupBarrierConfiguration {
        @Bean
        RepositoryLookupBarrier repositoryLookupBarrier() {
            return new RepositoryLookupBarrier();
        }

        @Bean
        @Primary
        AgentConversationRepository barrierAwareConversationRepository(
                @Qualifier("agentConversationRepository") AgentConversationRepository repository,
                RepositoryLookupBarrier barrier
        ) {
            return barrier.decorate(AgentConversationRepository.class, repository);
        }

        @Bean
        @Primary
        AgentTurnRequestRepository barrierAwareTurnRequestRepository(
                @Qualifier("agentTurnRequestRepository") AgentTurnRequestRepository repository,
                RepositoryLookupBarrier barrier
        ) {
            return barrier.decorate(AgentTurnRequestRepository.class, repository);
        }
    }

    static class RepositoryLookupBarrier {
        private volatile LookupGate activeGate;

        <T> T decorate(Class<T> repositoryType, T target) {
            Object proxy = Proxy.newProxyInstance(
                    repositoryType.getClassLoader(),
                    new Class<?>[] {repositoryType},
                    (instance, method, arguments) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "equals" -> instance == arguments[0];
                                case "hashCode" -> System.identityHashCode(instance);
                                case "toString" -> "LookupBarrier[" + repositoryType.getSimpleName() + "]";
                                default -> throw new UnsupportedOperationException(method.getName());
                            };
                        }
                        Object result;
                        try {
                            result = method.invoke(target, arguments);
                        } catch (InvocationTargetException exception) {
                            throw exception.getCause();
                        }
                        awaitIfArmed(method, arguments);
                        return result;
                    }
            );
            return repositoryType.cast(proxy);
        }

        void arm(String methodName, String... expectedArguments) {
            activeGate = new LookupGate(methodName, List.of(expectedArguments));
        }

        boolean awaitBothCalls() throws InterruptedException {
            LookupGate gate = Objects.requireNonNull(activeGate, "No concurrent lookup gate was armed");
            try {
                return gate.arrivals().await(10, TimeUnit.SECONDS);
            } finally {
                gate.release().countDown();
            }
        }

        private void awaitIfArmed(Method method, Object[] arguments) throws InterruptedException {
            LookupGate gate = activeGate;
            if (gate == null || !gate.matches(method, arguments) || gate.invocationCount().getAndIncrement() >= 2) {
                return;
            }
            gate.arrivals().countDown();
            if (!gate.release().await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting to release concurrent persistence lookups");
            }
        }
    }

    private record LookupGate(
            String methodName,
            List<String> expectedArguments,
            CountDownLatch arrivals,
            CountDownLatch release,
            AtomicInteger invocationCount
    ) {
        private LookupGate(String methodName, List<String> expectedArguments) {
            this(methodName, expectedArguments, new CountDownLatch(2), new CountDownLatch(1), new AtomicInteger());
        }

        boolean matches(Method method, Object[] actualArguments) {
            if (!methodName.equals(method.getName()) || actualArguments == null
                    || actualArguments.length != expectedArguments.size()) {
                return false;
            }
            for (int index = 0; index < actualArguments.length; index++) {
                if (!Objects.equals(expectedArguments.get(index), actualArguments[index])) {
                    return false;
                }
            }
            return true;
        }
    }
}
