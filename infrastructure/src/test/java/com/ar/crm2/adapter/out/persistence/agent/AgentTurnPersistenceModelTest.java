package com.ar.crm2.adapter.out.persistence.agent;

import com.ar.crm2.adapter.out.persistence.agent.entity.AgentConversationEntity;
import com.ar.crm2.adapter.out.persistence.agent.entity.AgentTurnEntity;
import com.ar.crm2.adapter.out.persistence.agent.entity.AgentTurnRequestEntity;
import com.ar.crm2.adapter.out.persistence.agent.entity.AgentVisibleHistoryEntity;
import com.ar.crm2.adapter.out.persistence.agent.mapper.AgentTurnPersistenceMapper;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentConversationRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentTurnRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentTurnRequestRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentVisibleHistoryRepository;
import com.ar.crm2.model.agent.entity.AgentTurn;
import com.ar.crm2.model.agent.entity.Conversation;
import com.ar.crm2.model.agent.enums.TurnState;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.ConversationId;
import com.ar.crm2.model.agent.vo.TurnId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:agent-turn-persistence;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AgentTurnPersistenceModelTest {

    @Autowired
    private AgentConversationRepository conversationRepository;

    @Autowired
    private AgentTurnRepository turnRepository;

    @Autowired
    private AgentTurnRequestRepository requestRepository;

    @Autowired
    private AgentVisibleHistoryRepository historyRepository;

    @Test
    void enforcesOneConversationPerOwner() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 20, 10, 0);
        conversationRepository.saveAndFlush(conversation("owner-a", createdAt));

        assertThrows(DataIntegrityViolationException.class, () ->
            conversationRepository.saveAndFlush(conversation("owner-a", createdAt.plusSeconds(1))));
    }

    @Test
    void scopesIdempotencyKeysToTheirOwner() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 20, 10, 0);
        AgentConversationEntity ownerAConversation = conversationRepository.saveAndFlush(conversation("owner-a", createdAt));
        AgentTurnEntity ownerATurn = turnRepository.saveAndFlush(turn(ownerAConversation, TurnState.PREPARED, createdAt));
        requestRepository.saveAndFlush(request("owner-a", "request-key", ownerATurn, createdAt));
        AgentTurnEntity duplicateKeyTurn = turnRepository.saveAndFlush(
            turn(ownerAConversation, TurnState.PREPARED, createdAt.plusSeconds(1)));

        assertThrows(DataIntegrityViolationException.class, () ->
            requestRepository.saveAndFlush(request("owner-a", "request-key", duplicateKeyTurn, createdAt.plusSeconds(1))));
    }

    @Test
    void allowsTheSameIdempotencyKeyForDifferentOwners() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 20, 10, 0);
        AgentConversationEntity ownerAConversation = conversationRepository.saveAndFlush(conversation("owner-a", createdAt));
        AgentTurnEntity ownerATurn = turnRepository.saveAndFlush(turn(ownerAConversation, TurnState.PREPARED, createdAt));
        requestRepository.saveAndFlush(request("owner-a", "request-key", ownerATurn, createdAt));
        AgentConversationEntity ownerBConversation = conversationRepository.saveAndFlush(conversation("owner-b", createdAt));
        AgentTurnEntity ownerBTurn = turnRepository.saveAndFlush(turn(ownerBConversation, TurnState.PREPARED, createdAt));
        requestRepository.saveAndFlush(request("owner-b", "request-key", ownerBTurn, createdAt));

        assertEquals(2, requestRepository.count());
    }

    @Test
    void storesTurnStateAndReconstitutesPersistedIdentityLinksAndTimestamps() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 20, 10, 0);
        LocalDateTime updatedAt = createdAt.plusMinutes(2);
        AgentConversationEntity conversation = conversationRepository.saveAndFlush(conversation("owner-a", createdAt));
        AgentTurnEntity persistedTurn = turnRepository.saveAndFlush(turn(conversation, TurnState.COMPLETED, createdAt, updatedAt));
        historyRepository.saveAndFlush(new AgentVisibleHistoryEntity(
            UUID.randomUUID().toString(), conversation, persistedTurn, "ASSISTANT", "completed response", updatedAt));
        turnRepository.flush();
        conversationRepository.flush();

        AgentTurnEntity reloadedTurn = turnRepository.findById(persistedTurn.getId()).orElseThrow();
        Conversation reconstitutedConversation = AgentTurnPersistenceMapper.toDomain(reloadedTurn.getConversation());
        AgentTurn reconstitutedTurn = AgentTurnPersistenceMapper.toDomain(reloadedTurn);

        assertEquals(persistedTurn.getId(), reconstitutedTurn.getId().value().toString());
        assertEquals(reloadedTurn.getConversation().getId(), reconstitutedTurn.getConversationId().value().toString());
        assertEquals(TurnState.COMPLETED, reconstitutedTurn.getState());
        assertEquals(createdAt, reloadedTurn.getCreatedAt());
        assertEquals(updatedAt, reloadedTurn.getUpdatedAt());
        assertEquals(AgentOwnerId.from("owner-a"), reconstitutedConversation.getOwnerId());
        assertEquals(ConversationId.from(UUID.fromString(reloadedTurn.getConversation().getId())), reconstitutedConversation.getId());
        assertEquals(TurnId.from(UUID.fromString(reloadedTurn.getId())), reconstitutedTurn.getId());
        assertEquals(1, historyRepository.count());
    }

    private AgentConversationEntity conversation(String ownerId, LocalDateTime createdAt) {
        return new AgentConversationEntity(UUID.randomUUID().toString(), ownerId, createdAt);
    }

    private AgentTurnEntity turn(AgentConversationEntity conversation, TurnState state, LocalDateTime createdAt) {
        return turn(conversation, state, createdAt, createdAt);
    }

    private AgentTurnEntity turn(
            AgentConversationEntity conversation,
            TurnState state,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new AgentTurnEntity(UUID.randomUUID().toString(), conversation, state, createdAt, updatedAt);
    }

    private AgentTurnRequestEntity request(
            String ownerId,
            String idempotencyKey,
            AgentTurnEntity turn,
            LocalDateTime createdAt
    ) {
        return new AgentTurnRequestEntity(
            UUID.randomUUID().toString(), ownerId, idempotencyKey, "fingerprint", turn, createdAt);
    }
}
