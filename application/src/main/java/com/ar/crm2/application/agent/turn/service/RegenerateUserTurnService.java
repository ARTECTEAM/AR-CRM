package com.ar.crm2.application.agent.turn.service;

import com.ar.crm2.application.agent.turn.command.RegenerateUserTurnCommand;
import com.ar.crm2.application.agent.turn.port.in.RegenerateUserTurnUseCase;
import com.ar.crm2.application.agent.turn.port.out.ChatCompletionPort;
import com.ar.crm2.application.agent.turn.port.out.CompleteRegeneratedTurnPort;
import com.ar.crm2.application.agent.turn.port.out.CreateRegenerationPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedVisibleHistoryPort;
import com.ar.crm2.application.agent.turn.port.out.FindEligibleDurableMemoriesPort;
import com.ar.crm2.application.agent.turn.port.out.FindUserTurnContentPort;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/** Coordinates sequential regeneration without persistence or provider details. */
@RequiredArgsConstructor
public final class RegenerateUserTurnService implements RegenerateUserTurnUseCase {
    private final CreateRegenerationPort createRegenerationPort;
    private final FindCompletedVisibleHistoryPort findCompletedVisibleHistoryPort;
    private final FindUserTurnContentPort findUserTurnContentPort;
    private final FindEligibleDurableMemoriesPort findEligibleDurableMemoriesPort;
    private final CompleteRegeneratedTurnPort completeRegeneratedTurnPort;
    private final ChatCompletionPort chatCompletionPort;

    @Override
    public String regenerate(RegenerateUserTurnCommand command) {
        AgentOwnerId ownerId = AgentOwnerId.from(command.actorSubject());
        TurnId turnId = TurnId.from(command.turnId());
        Optional<String> canonicalContent = createRegenerationPort.createRegenerationOrFindCanonical(
                ownerId, turnId, command.opaqueHandle(), command.idempotencyKey());
        if (canonicalContent.isPresent()) {
            return canonicalContent.get();
        }
        List<String> visibleHistory = findCompletedVisibleHistoryPort.findCompletedVisibleHistory(
                ownerId, turnId, command.opaqueHandle(), command.visibleHistoryLimit());
        String userContent = findUserTurnContentPort.findUserTurnContent(ownerId, turnId, command.opaqueHandle());
        List<String> durableMemories = findEligibleDurableMemoriesPort.findEligibleDurableMemories(ownerId);
        String assistantContent = chatCompletionPort.complete(
                ownerId, turnId, visibleHistory, durableMemories, userContent);
        return completeRegeneratedTurnPort.completeRegeneratedTurn(
                ownerId, turnId, command.opaqueHandle(), command.idempotencyKey(), assistantContent);
    }

}
