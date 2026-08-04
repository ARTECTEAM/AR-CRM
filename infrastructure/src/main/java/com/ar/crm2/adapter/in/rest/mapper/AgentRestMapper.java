package com.ar.crm2.adapter.in.rest.mapper;

import com.ar.crm2.adapter.in.rest.dto.request.AgentMessageRequest;
import com.ar.crm2.application.agent.turn.command.CompleteUserTurnCommand;
import com.ar.crm2.application.agent.turn.command.CreateUserTurnCommand;
import com.ar.crm2.application.security.ActorContext;
import com.ar.crm2.application.security.exception.AuthenticatedUsuarioRequiredException;
import com.ar.crm2.model.agent.vo.AcceptedUserTurn;
import com.ar.crm2.security.ActorContextRequestAttributeFilter;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/**
 * Authenticated-DTO → Application-command mapper for the Pipely CRM
 * conversational ingress. Sole bridge between the controller and the
 * Application layer; identity is sourced ONLY from the validated JWT
 * context ({@link ActorContextRequestAttributeFilter}). Static-only
 * utility — no Spring or DI concerns; called once per request after
 * Spring's validation and after {@link ActorContext} has been resolved.
 */
public final class AgentRestMapper {

    private AgentRestMapper() {
    }

    /**
     * Derives the trusted {@link ActorContext} from the request attribute
     * populated by {@link ActorContextRequestAttributeFilter}.
     *
     * @throws AuthenticatedUsuarioRequiredException when the attribute is
     *         absent or carries no subject — never default to anonymous.
     */
    public static ActorContext requireAuthenticatedActor(HttpServletRequest request) {
        Object attribute = request.getAttribute(
                ActorContextRequestAttributeFilter.ACTOR_CONTEXT_ATTRIBUTE);
        if (!(attribute instanceof ActorContext actorContext) || actorContext.subject() == null) {
            throw AuthenticatedUsuarioRequiredException.forMissingActorContext();
        }
        return actorContext;
    }

    /**
     * Resolves the trusted CRM {@code actorUsuarioId} from the validated
     * JWT claim. Missing claims fail closed — any caller lacking a
     * {@code usuario_id} claim MUST NOT reach the chat-completion path.
     */
    public static UUID requireActorUsuarioId(ActorContext actorContext) {
        return actorContext.usuarioId()
                .orElseThrow(AuthenticatedUsuarioRequiredException::forMissingUsuarioId);
    }

    public static CreateUserTurnCommand toCreateTurnCommand(ActorContext actor, AgentMessageRequest request) {
        return new CreateUserTurnCommand(actor.subject(), request.idempotencyKey(), request.message());
    }

    public static CompleteUserTurnCommand toCompleteTurnCommand(
            ActorContext actor, UUID actorUsuarioId, AcceptedUserTurn accepted,
            String prompt, int visibleHistoryLimit
    ) {
        return new CompleteUserTurnCommand(
                actor.subject(), actorUsuarioId,
                accepted.turn().getId().value(), accepted.opaqueHandle(),
                prompt, visibleHistoryLimit);
    }
}