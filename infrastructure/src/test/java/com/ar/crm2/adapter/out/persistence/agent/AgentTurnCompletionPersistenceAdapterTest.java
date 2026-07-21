package com.ar.crm2.adapter.out.persistence.agent;

import com.ar.crm2.adapter.out.persistence.agent.entity.AgentConversationEntity;
import com.ar.crm2.adapter.out.persistence.agent.entity.AgentTurnEntity;
import com.ar.crm2.adapter.out.persistence.agent.entity.AgentTurnRequestEntity;
import com.ar.crm2.adapter.out.persistence.agent.entity.AgentVisibleHistoryEntity;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentConversationRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentTurnRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentTurnRequestRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentVisibleHistoryRepository;
import com.ar.crm2.model.agent.enums.TurnState;
import com.ar.crm2.model.agent.enums.VisibleMessageRole;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.agent.vo.VisibleMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(AgentTurnAdapter.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:agent-turn-completion;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AgentTurnCompletionPersistenceAdapterTest {

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

    @AfterEach
    void clearPersistenceState() {
        historyRepository.deleteAll();
        requestRepository.deleteAll();
        turnRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    @Test
    void findsCanonicalCompletedAssistantContentOnlyForTheOwnerTurnAndHandle() {
        PersistedTurn completed = persistedTurn("owner-a", TurnState.COMPLETED, 1);
        visible(completed, "USER", "question", 1);
        visible(completed, "ASSISTANT", "canonical answer", 2);

        assertThat(adapter.findCompletedAssistantContent(completed.ownerId(), completed.turnId(), completed.handle()))
                .contains("canonical answer");
        assertThat(adapter.findCompletedAssistantContent(AgentOwnerId.from("owner-b"), completed.turnId(), completed.handle()))
                .isEmpty();
    }

    @Test
    void returnsOrderedRoleBearingHistoryOldestFirstAndExcludesTheActiveTurn() {
        PersistedTurn first = persistedTurn("owner-a", TurnState.COMPLETED, 1);
        visible(first, "USER", "first user", 1);
        visible(first, "ASSISTANT", "first assistant", 2);
        PersistedTurn second = persistedTurn("owner-a", TurnState.COMPLETED, 3);
        visible(second, "USER", "second user", 3);
        visible(second, "ASSISTANT", "second assistant", 4);
        PersistedTurn active = persistedTurn("owner-a", TurnState.PREPARED, 5);
        visible(active, "USER", "active user", 5);
        PersistedTurn otherOwner = persistedTurn("owner-b", TurnState.COMPLETED, 6);
        visible(otherOwner, "ASSISTANT", "other owner", 6);

        List<VisibleMessage> history = adapter.findCompletedVisibleHistory(
                active.ownerId(), active.turnId(), active.handle(), 3);

        assertThat(history).containsExactly(
                VisibleMessage.assistant("first assistant"),
                VisibleMessage.user("second user"),
                VisibleMessage.assistant("second assistant")
        );
    }

    @Test
    void preservesUserAndAssistantSpeakerProvenanceWhenMappingFromStorage() {
        PersistedTurn first = persistedTurn("owner-a", TurnState.COMPLETED, 1);
        visible(first, "USER", "first user", 1);
        visible(first, "ASSISTANT", "first assistant", 2);
        PersistedTurn active = persistedTurn("owner-a", TurnState.PREPARED, 3);
        visible(active, "USER", "active user", 3);

        List<VisibleMessage> history = adapter.findCompletedVisibleHistory(
                active.ownerId(), active.turnId(), active.handle(), 5);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).role()).isEqualTo(VisibleMessageRole.USER);
        assertThat(history.get(0).content()).isEqualTo("first user");
        assertThat(history.get(1).role()).isEqualTo(VisibleMessageRole.ASSISTANT);
        assertThat(history.get(1).content()).isEqualTo("first assistant");
    }

    @Test
    void returnsEmptyRoleBearingHistoryWhenTheActiveTurnHasNoCompletedPredecessors() {
        PersistedTurn active = persistedTurn("owner-a", TurnState.PREPARED, 1);
        visible(active, "USER", "active user", 1);

        List<VisibleMessage> history = adapter.findCompletedVisibleHistory(
                active.ownerId(), active.turnId(), active.handle(), 5);

        assertThat(history).isEmpty();
    }

    @Test
    void completesPreparedTurnsOnceAndReturnsThePersistedCanonicalAssistantContentOnRetry() {
        PersistedTurn prepared = persistedTurn("owner-a", TurnState.PREPARED, 1);
        visible(prepared, "USER", "question", 1);

        String completed = adapter.completePreparedTurn(
                prepared.ownerId(), prepared.turnId(), prepared.handle(), "first assistant answer");
        String retried = adapter.completePreparedTurn(
                prepared.ownerId(), prepared.turnId(), prepared.handle(), "ignored retry answer");

        assertThat(completed).isEqualTo("first assistant answer");
        assertThat(retried).isEqualTo("first assistant answer");
        assertThat(turnRepository.findById(prepared.turnId().value().toString()).orElseThrow().getState())
                .isEqualTo(TurnState.COMPLETED);
        assertThat(historyRepository.findAll())
                .filteredOn(history -> history.getRole().equals("ASSISTANT"))
                .extracting(AgentVisibleHistoryEntity::getContent)
                .containsExactly("first assistant answer");
    }

    @Test
    void rollsBackTheStateTransitionWhenTheAssistantHistoryCannotBePersisted() {
        PersistedTurn prepared = persistedTurn("owner-a", TurnState.PREPARED, 1);

        assertThatThrownBy(() -> adapter.completePreparedTurn(
                prepared.ownerId(), prepared.turnId(), prepared.handle(), null))
                .isInstanceOf(RuntimeException.class);

        assertThat(turnRepository.findById(prepared.turnId().value().toString()).orElseThrow().getState())
                .isEqualTo(TurnState.PREPARED);
        assertThat(historyRepository.findAll())
                .filteredOn(history -> history.getRole().equals("ASSISTANT"))
                .isEmpty();
    }

    private PersistedTurn persistedTurn(String owner, TurnState state, int minute) {
        LocalDateTime timestamp = timestamp(minute);
        AgentConversationEntity conversation = conversationRepository.findByOwnerId(owner)
                .orElseGet(() -> conversationRepository.saveAndFlush(
                        new AgentConversationEntity(UUID.randomUUID().toString(), owner, timestamp)));
        AgentTurnEntity turn = turnRepository.saveAndFlush(
                new AgentTurnEntity(UUID.randomUUID().toString(), conversation, state, timestamp, timestamp));
        String handle = UUID.randomUUID().toString();
        requestRepository.saveAndFlush(new AgentTurnRequestEntity(
                UUID.randomUUID().toString(), owner, "key-" + UUID.randomUUID(), "fingerprint", handle, turn, timestamp));
        return new PersistedTurn(AgentOwnerId.from(owner), TurnId.from(UUID.fromString(turn.getId())), handle, conversation, turn);
    }

    private void visible(PersistedTurn turn, String role, String content, int minute) {
        historyRepository.saveAndFlush(new AgentVisibleHistoryEntity(
                UUID.randomUUID().toString(), turn.conversation(), turn.entity(), role, content, timestamp(minute)));
    }

    private LocalDateTime timestamp(int minute) {
        return LocalDateTime.of(2026, 7, 20, 10, minute);
    }

    private record PersistedTurn(
            AgentOwnerId ownerId,
            TurnId turnId,
            String handle,
            AgentConversationEntity conversation,
            AgentTurnEntity entity
    ) {
    }
}
