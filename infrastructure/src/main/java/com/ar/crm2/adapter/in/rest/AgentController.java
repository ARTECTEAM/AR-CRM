package com.ar.crm2.adapter.in.rest;

import com.ar.crm2.adapter.in.rest.dto.request.AgentMessageRequest;
import com.ar.crm2.adapter.in.rest.dto.response.AgentMessageResponse;
import com.ar.crm2.adapter.in.rest.mapper.AgentRestMapper;
import com.ar.crm2.application.agent.turn.command.CompleteUserTurnCommand;
import com.ar.crm2.application.agent.turn.command.CreateUserTurnCommand;
import com.ar.crm2.application.agent.turn.port.in.CompleteUserTurnUseCase;
import com.ar.crm2.application.agent.turn.port.in.CreateUserTurnUseCase;
import com.ar.crm2.application.security.ActorContext;
import com.ar.crm2.model.agent.vo.AcceptedUserTurn;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Single authenticated REST ingress for the Pipely CRM conversational agent.
 * Exposes ONLY {@code POST /api/agent/messages} — internal completion,
 * regeneration, and tool execution remain private to the Application layer.
 *
 * <p>Identity is sourced exclusively from the JWT-derived
 * {@link ActorContext} (see {@code ActorContextRequestAttributeFilter});
 * the controller never reads identity from the request body or the model.
 * Idempotent retry convergence is the responsibility of the Application
 * turn services; the controller faithfully forwards the request and lets
 * the Application adapters persist the canonical record.
 */
@RestController
@RequestMapping("/api/agent/messages")
@RequiredArgsConstructor
public final class AgentController {

    /** Bounded visible-history window fed to the chat completion.
     *  Mirrors the documented {@code find_contacts} cap (20). */
    static final int VISIBLE_HISTORY_LIMIT = 20;

    private final CreateUserTurnUseCase createUserTurnUseCase;
    private final CompleteUserTurnUseCase completeUserTurnUseCase;

    @PostMapping
    public ResponseEntity<AgentMessageResponse> postAgentMessage(
            HttpServletRequest httpRequest,
            @Valid @RequestBody AgentMessageRequest request
    ) {
        ActorContext actor = AgentRestMapper.requireAuthenticatedActor(httpRequest);
        UUID actorUsuarioId = AgentRestMapper.requireActorUsuarioId(actor);

        CreateUserTurnCommand createCommand =
                AgentRestMapper.toCreateTurnCommand(actor, request);
        AcceptedUserTurn accepted = createUserTurnUseCase.create(createCommand);

        CompleteUserTurnCommand completeCommand = AgentRestMapper.toCompleteTurnCommand(
                actor, actorUsuarioId, accepted, request.message(), VISIBLE_HISTORY_LIMIT);
        String content = completeUserTurnUseCase.complete(completeCommand);

        return ResponseEntity.ok(AgentMessageResponse.of(content));
    }
}