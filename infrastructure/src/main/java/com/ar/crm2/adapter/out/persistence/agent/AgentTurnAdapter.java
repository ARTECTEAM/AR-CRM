package com.ar.crm2.adapter.out.persistence.agent;

import com.ar.crm2.adapter.out.persistence.agent.entity.AgentConversationEntity;
import com.ar.crm2.adapter.out.persistence.agent.entity.AgentTurnEntity;
import com.ar.crm2.adapter.out.persistence.agent.entity.AgentTurnRequestEntity;
import com.ar.crm2.adapter.out.persistence.agent.entity.AgentVisibleHistoryEntity;
import com.ar.crm2.adapter.out.persistence.agent.mapper.AgentTurnMapper;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentConversationRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentTurnRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentTurnRequestRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentVisibleHistoryRepository;
import com.ar.crm2.application.agent.turn.exception.IdempotencyKeyReusedException;
import com.ar.crm2.application.agent.turn.port.out.CompletePreparedTurnPort;
import com.ar.crm2.application.agent.turn.port.out.CreateUserTurnPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedAssistantContentPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedVisibleHistoryPort;
import com.ar.crm2.model.agent.entity.AgentTurn;
import com.ar.crm2.model.agent.entity.Conversation;
import com.ar.crm2.model.agent.enums.TurnState;
import com.ar.crm2.model.agent.vo.AcceptedUserTurn;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persists or converges the complete write-side state of a user submission. */
@RequiredArgsConstructor
public class AgentTurnAdapter implements CreateUserTurnPort,
        FindCompletedAssistantContentPort,
        FindCompletedVisibleHistoryPort,
        CompletePreparedTurnPort {
    private static final String USER_ROLE = "USER";
    private static final String ASSISTANT_ROLE = "ASSISTANT";

    private final AgentConversationRepository conversationRepository;
    private final AgentTurnRepository turnRepository;
    private final AgentTurnRequestRepository requestRepository;
    private final AgentVisibleHistoryRepository historyRepository;

    @Override
    @Transactional
    public AcceptedUserTurn createOrGetUserTurn(
            Conversation conversation,
            AgentTurn turn,
            AgentOwnerId ownerId,
            String idempotencyKey,
            String originalUserContent,
            String payloadFingerprint,
            String opaqueHandle
    ) {
        return requestRepository.findByOwnerIdAndIdempotencyKey(ownerId.value(), idempotencyKey)
                .map(request -> canonicalReceipt(request, payloadFingerprint))
                .orElseGet(() -> persistNewReceipt(
                        conversation,
                        turn,
                        ownerId,
                        idempotencyKey,
                        originalUserContent,
                        payloadFingerprint,
                        opaqueHandle
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findCompletedAssistantContent(
            AgentOwnerId ownerId,
            TurnId turnId,
            String opaqueHandle
    ) {
        return findRequest(ownerId, turnId, opaqueHandle)
                .filter(request -> request.getTurn().getState() == TurnState.COMPLETED)
                .flatMap(request -> findAssistantContent(request.getTurn().getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findCompletedVisibleHistory(
            AgentOwnerId ownerId,
            TurnId turnId,
            String opaqueHandle,
            int maximumMessages
    ) {
        if (maximumMessages <= 0 || findRequest(ownerId, turnId, opaqueHandle).isEmpty()) {
            return List.of();
        }
        List<AgentVisibleHistoryEntity> newestFirst = historyRepository
                .findByConversationOwnerIdAndTurnStateAndTurnIdNotOrderByVisibleAtDesc(
                        ownerId.value(),
                        TurnState.COMPLETED,
                        turnId.value().toString(),
                        PageRequest.of(0, maximumMessages)
                );
        List<String> oldestFirst = new ArrayList<>(newestFirst.size());
        for (int index = newestFirst.size() - 1; index >= 0; index--) {
            oldestFirst.add(newestFirst.get(index).getContent());
        }
        return oldestFirst;
    }

    @Override
    @Transactional
    public String completePreparedTurn(
            AgentOwnerId ownerId,
            TurnId turnId,
            String opaqueHandle,
            String assistantContent
    ) {
        AgentTurnRequestEntity request = findRequest(ownerId, turnId, opaqueHandle).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        int updatedRows = turnRepository.transitionPreparedToCompleted(
                turnId.value().toString(), ownerId.value(), TurnState.PREPARED, TurnState.COMPLETED, now);
        if (updatedRows == 0) {
            return findAssistantContent(turnId.value().toString()).orElseThrow();
        }
        historyRepository.saveAndFlush(new AgentVisibleHistoryEntity(
                UUID.randomUUID().toString(),
                request.getTurn().getConversation(),
                request.getTurn(),
                ASSISTANT_ROLE,
                assistantContent,
                now
        ));
        return assistantContent;
    }

    private AcceptedUserTurn canonicalReceipt(AgentTurnRequestEntity request, String payloadFingerprint) {
        if (!request.getFingerprint().equals(payloadFingerprint)) {
            throw new IdempotencyKeyReusedException();
        }
        return new AcceptedUserTurn(AgentTurnMapper.toDomain(request.getTurn()), request.getOpaqueHandle());
    }

    private Optional<AgentTurnRequestEntity> findRequest(
            AgentOwnerId ownerId,
            TurnId turnId,
            String opaqueHandle
    ) {
        return requestRepository.findByOwnerIdAndTurnIdAndOpaqueHandle(
                ownerId.value(), turnId.value().toString(), opaqueHandle);
    }

    private Optional<String> findAssistantContent(String turnId) {
        return historyRepository.findFirstByTurnIdAndRoleOrderByVisibleAtDesc(turnId, ASSISTANT_ROLE)
                .map(AgentVisibleHistoryEntity::getContent);
    }

    private AcceptedUserTurn persistNewReceipt(
            Conversation candidateConversation,
            AgentTurn candidateTurn,
            AgentOwnerId ownerId,
            String idempotencyKey,
            String originalUserContent,
            String payloadFingerprint,
            String opaqueHandle
    ) {
        LocalDateTime now = LocalDateTime.now();
        AgentConversationEntity conversation = conversationRepository.findByOwnerId(ownerId.value())
                .orElseGet(() -> conversationRepository.save(
                        AgentTurnMapper.toEntity(candidateConversation, now)));
        AgentTurnEntity turn = turnRepository.save(AgentTurnMapper.toEntity(candidateTurn, conversation, now, now));
        requestRepository.save(new AgentTurnRequestEntity(
                UUID.randomUUID().toString(),
                ownerId.value(),
                idempotencyKey,
                payloadFingerprint,
                opaqueHandle,
                turn,
                now
        ));
        historyRepository.saveAndFlush(new AgentVisibleHistoryEntity(
                UUID.randomUUID().toString(),
                conversation,
                turn,
                USER_ROLE,
                originalUserContent,
                now
        ));
        return new AcceptedUserTurn(AgentTurnMapper.toDomain(turn), opaqueHandle);
    }
}
