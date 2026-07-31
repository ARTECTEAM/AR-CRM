package com.ar.crm2.application.agent.turn.port.out;

import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.agent.vo.VisibleMessage;

import java.util.List;
import java.util.UUID;

/**
 * Completes a prompt using trusted conversation context. The visible history
 * arrives already ordered oldest-first and explicitly tagged with the speaker
 * provenance; the adapter must not interpret the role field as authorization.
 *
 * <p>The contract carries BOTH owner and trusted CRM actor identity as
 * separate, non-interchangeable parameters:
 * <ul>
 *   <li>{@code ownerId} is the conversation owner used for history,
 *       memory, and turn-key scoping (unchanged by A3).</li>
 *   <li>{@code actorUsuarioId} is the trusted CRM actor identity required
 *       by the Application layer as the security scope for CRM-side
 *       effects. It is supplied by the Agent ingress (JWT/ActorContext)
 *       and forwarded unchanged from the Command to the adapter —
 *       it MUST be carried as a raw {@link UUID} and never derived from
 *       the model, prompt, or tool arguments.</li>
 * </ul>
 */
@FunctionalInterface
public interface ChatCompletionPort {

    String complete(
            AgentOwnerId ownerId,
            UUID actorUsuarioId,
            TurnId turnId,
            List<VisibleMessage> visibleHistory,
            List<String> durableMemories,
            String normalizedPrompt
    );
}
