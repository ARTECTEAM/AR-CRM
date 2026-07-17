package com.ar.crm2.application.agent.turn.service;

import com.ar.crm2.application.agent.turn.command.CreateUserTurnCommand;
import com.ar.crm2.application.agent.turn.port.in.CreateUserTurnUseCase;
import com.ar.crm2.application.agent.turn.port.out.CreateUserTurnPort;
import com.ar.crm2.model.agent.entity.AgentTurn;
import com.ar.crm2.model.agent.entity.Conversation;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/** Coordinates submitted user-turn candidates without persistence details. */
@RequiredArgsConstructor
public final class CreateUserTurnService implements CreateUserTurnUseCase {
    private final CreateUserTurnPort createUserTurnPort;

    @Override
    public AgentTurn create(CreateUserTurnCommand command) {
        AgentOwnerId ownerId = AgentOwnerId.from(command.actorSubject());
        Conversation conversation = Conversation.create(ownerId);
        AgentTurn turn = conversation.createTurn(TurnId.create());
        return createUserTurnPort.createOrGetUserTurn(
                conversation,
                turn,
                ownerId,
                command.idempotencyKey(),
                fingerprint(command.prompt()),
                UUID.randomUUID().toString()
        );
    }

    private static String fingerprint(String prompt) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(prompt.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
