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
import com.ar.crm2.application.agent.turn.port.out.CreateUserTurnPort;
import com.ar.crm2.model.agent.entity.AgentTurn;
import com.ar.crm2.model.agent.entity.Conversation;
import com.ar.crm2.model.agent.vo.AcceptedUserTurn;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/** Persists or converges the complete write-side state of a user submission. */
@RequiredArgsConstructor
public class AgentTurnAdapter implements CreateUserTurnPort {
    private static final String USER_ROLE = "USER";

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

    private AcceptedUserTurn canonicalReceipt(AgentTurnRequestEntity request, String payloadFingerprint) {
        if (!request.getFingerprint().equals(payloadFingerprint)) {
            throw new IdempotencyKeyReusedException();
        }
        return new AcceptedUserTurn(AgentTurnMapper.toDomain(request.getTurn()), request.getOpaqueHandle());
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
