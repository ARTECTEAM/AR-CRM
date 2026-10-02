package com.ar.crm2.application.agent.turn.service;

import com.ar.crm2.application.agent.turn.command.RegenerateUserTurnCommand;
import com.ar.crm2.application.agent.turn.port.in.RegenerateUserTurnUseCase;
import com.ar.crm2.application.agent.turn.port.out.ChatCompletionPort;
import com.ar.crm2.application.agent.turn.port.out.CompleteRegeneratedTurnPort;
import com.ar.crm2.application.agent.turn.port.out.CreateRegenerationPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedVisibleHistoryPort;
import com.ar.crm2.application.agent.turn.port.out.FindEligibleDurableMemoriesPort;
import com.ar.crm2.application.agent.turn.port.out.FindUserTurnContentPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.AuthorizationCapabilities;
import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.agent.vo.VisibleMessage;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

/**
 * Coordinates sequential regeneration without persistence or provider details.
 *
 * <p>Threads the trusted CRM {@code actorUsuarioId} from the Command to
 * the completion port as a separate, non-overridable parameter, in the
 * same posture as {@link CompleteUserTurnService}.
 */
@RequiredArgsConstructor
public class RegenerateUserTurnService implements RegenerateUserTurnUseCase {
    private final CreateRegenerationPort createRegenerationPort;
    private final FindCompletedVisibleHistoryPort findCompletedVisibleHistoryPort;
    private final FindUserTurnContentPort findUserTurnContentPort;
    private final FindEligibleDurableMemoriesPort findEligibleDurableMemoriesPort;
    private final CompleteRegeneratedTurnPort completeRegeneratedTurnPort;
    private final ChatCompletionPort chatCompletionPort;
    private final CrmAuthorization authorization;
    private final CurrentActorPort currentActorPort;

    @Override
    public String regenerate(RegenerateUserTurnCommand command) {
        AgentOwnerId ownerId = AgentOwnerId.from(command.actorSubject());
        String authorizationRevision = authorization.revision();
        CurrentActor actor = currentActorPort.currentActor()
                .orElseThrow(() -> new CrmActorUnavailableException("Authenticated CRM user is not active"));
        UUID actorUsuarioId = actor.usuarioId();
        TurnId turnId = TurnId.from(command.turnId());
        Optional<String> canonicalContent = createRegenerationPort.createRegenerationOrFindCanonical(
                ownerId, turnId, command.opaqueHandle(), command.idempotencyKey(), authorizationRevision);
        if (canonicalContent.isPresent()) {
            requireAuthorizationRevision(authorizationRevision,
                    "CRM permissions changed during agent regeneration; start a new turn");
            return canonicalContent.get();
        }
        List<VisibleMessage> visibleHistory = findCompletedVisibleHistoryPort.findCompletedVisibleHistory(
                ownerId, turnId, command.opaqueHandle(), command.visibleHistoryLimit(), authorizationRevision);
        String userContent = findUserTurnContentPort.findUserTurnContent(
                ownerId, turnId, command.opaqueHandle(), authorizationRevision);
        List<String> durableMemories = findEligibleDurableMemoriesPort.findEligibleDurableMemories(
                ownerId, authorizationRevision);
        AuthorizationCapabilities capabilities = Objects.requireNonNullElse(
                authorization.authorizationCapabilities(), AuthorizationCapabilities.none());
        requireAuthorizationRevision(authorizationRevision,
                "CRM permissions changed while preparing agent context; start a new turn");
        String assistantContent = chatCompletionPort.complete(
                ownerId, actorUsuarioId, command.actorSuperUsuarioId(), capabilities, turnId,
                visibleHistory, durableMemories, userContent);
        requireAuthorizationRevision(authorizationRevision,
                "CRM permissions changed during agent regeneration; start a new turn");
        return completeRegeneratedTurnPort.completeRegeneratedTurn(
                ownerId, turnId, command.opaqueHandle(), command.idempotencyKey(),
                authorizationRevision, assistantContent);
    }

    private void requireAuthorizationRevision(String expected, String message) {
        if (!expected.equals(authorization.revision())) {
            throw new CrmActorUnavailableException(message);
        }
    }

}
