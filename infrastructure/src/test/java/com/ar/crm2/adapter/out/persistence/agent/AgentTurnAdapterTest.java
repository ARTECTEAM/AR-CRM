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
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(AgentTurnAdapter.class)
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
}
