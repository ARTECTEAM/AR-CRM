package com.ar.crm2.application.agent.turn.service;

import com.ar.crm2.application.agent.turn.command.CompleteUserTurnCommand;
import com.ar.crm2.application.agent.turn.port.in.CompleteUserTurnUseCase;
import com.ar.crm2.application.agent.turn.port.out.ChatCompletionPort;
import com.ar.crm2.application.agent.turn.port.out.CompletePreparedTurnPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedAssistantContentPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedVisibleHistoryPort;
import com.ar.crm2.application.agent.turn.port.out.FindEligibleDurableMemoriesPort;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.agent.vo.VisibleMessage;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Coordinates completion retry convergence without persistence or provider details.
 *
 * <p>Threads the trusted CRM {@code actorUsuarioId} from the Command to
 * the completion port as a separate, non-overridable parameter. The
 * owner subject remains the conversation/handle scope; the actor
 * {@code UUID} is the security scope for any CRM-side effect (e.g. the
 * future {@code find_contacts} tool) and is forwarded unchanged.
 */
@RequiredArgsConstructor
public class CompleteUserTurnService implements CompleteUserTurnUseCase {
    private final FindCompletedAssistantContentPort findCompletedAssistantContentPort;
    private final FindCompletedVisibleHistoryPort findCompletedVisibleHistoryPort;
    private final FindEligibleDurableMemoriesPort findEligibleDurableMemoriesPort;
    private final CompletePreparedTurnPort completePreparedTurnPort;
    private final ChatCompletionPort chatCompletionPort;

    @Override
    public String complete(CompleteUserTurnCommand command) {
        AgentOwnerId ownerId = AgentOwnerId.from(command.actorSubject());
        UUID actorUsuarioId = command.actorUsuarioId();
        TurnId turnId = TurnId.from(command.turnId());
        Optional<String> completedContent = findCompletedAssistantContentPort.findCompletedAssistantContent(
                ownerId, turnId, command.opaqueHandle());
        if (completedContent.isPresent()) {
            return completedContent.get();
        }
        List<VisibleMessage> visibleHistory = findCompletedVisibleHistoryPort.findCompletedVisibleHistory(
                ownerId, turnId, command.opaqueHandle(), command.visibleHistoryLimit());
        List<String> durableMemories = findEligibleDurableMemoriesPort.findEligibleDurableMemories(ownerId);
        String assistantContent = chatCompletionPort.complete(
                ownerId, actorUsuarioId, turnId, visibleHistory, durableMemories, command.prompt());
        return completePreparedTurnPort.completePreparedTurn(ownerId, turnId, command.opaqueHandle(), assistantContent);
    }

}
