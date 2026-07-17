package com.ar.crm2.application.agent.turn.service;

import com.ar.crm2.application.agent.turn.command.CreateUserTurnCommand;
import com.ar.crm2.application.agent.turn.exception.IdempotencyKeyReusedException;
import com.ar.crm2.application.agent.turn.port.out.CreateUserTurnPort;
import com.ar.crm2.model.agent.entity.AgentTurn;
import com.ar.crm2.model.agent.entity.Conversation;
import com.ar.crm2.model.agent.enums.TurnState;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateUserTurnServiceTest {

    @Test
    void createsDomainCandidatesFromSimpleCommandInputsAndDelegatesThemAtomically() {
        CapturingCreateUserTurnPort port = new CapturingCreateUserTurnPort();
        CreateUserTurnService service = new CreateUserTurnService(port);

        AgentTurn canonicalTurn = service.create(new CreateUserTurnCommand(" actor-a ", " key-1 ", "  Hello Pipely  "));

        assertSame(port.returnedTurn, canonicalTurn);
        assertEquals(AgentOwnerId.from("actor-a"), port.ownerId);
        assertEquals(AgentOwnerId.from("actor-a"), port.conversation.getOwnerId());
        assertEquals(port.conversation.getId(), port.turn.getConversationId());
        assertEquals(TurnState.PREPARED, port.turn.getState());
        assertEquals("key-1", port.idempotencyKey);
        assertEquals("a5008e1d8f2bbf5381a880e532c8810f01eb8c99593df4979fa6047d917640bf", port.payloadFingerprint);
        assertNotNull(port.opaqueHandle);
    }

    @Test
    void delegatesFreshCandidatesWhileAllowingAtomicPortConvergenceAndConflictPropagation() {
        ConvergingCreateUserTurnPort port = new ConvergingCreateUserTurnPort();
        CreateUserTurnService service = new CreateUserTurnService(port);

        AgentTurn first = service.create(new CreateUserTurnCommand("actor-a", "key-1", "prompt"));
        AgentTurn retried = service.create(new CreateUserTurnCommand("actor-a", "key-1", "prompt"));

        assertSame(first, retried);
        assertNotEquals(port.firstCandidate, port.secondCandidate);
        assertNotEquals(port.firstConversation, port.secondConversation);
        assertThrows(IdempotencyKeyReusedException.class, () -> new CreateUserTurnService(new ConflictingCreateUserTurnPort())
                .create(new CreateUserTurnCommand("actor-a", "key-1", "changed prompt")));
    }

    @Test
    void constructorAllowsNullPortBecauseDependencyValidationIsNotAnApplicationFlowGuard() {
        assertDoesNotThrow(() -> new CreateUserTurnService(null));
    }

    private static final class CapturingCreateUserTurnPort implements CreateUserTurnPort {
        private Conversation conversation;
        private AgentTurn turn;
        private AgentOwnerId ownerId;
        private String idempotencyKey;
        private String payloadFingerprint;
        private String opaqueHandle;
        private AgentTurn returnedTurn;

        @Override
        public AgentTurn createOrGetUserTurn(
                Conversation conversation,
                AgentTurn turn,
                AgentOwnerId ownerId,
                String idempotencyKey,
                String payloadFingerprint,
                String opaqueHandle
        ) {
            this.conversation = conversation;
            this.turn = turn;
            this.ownerId = ownerId;
            this.idempotencyKey = idempotencyKey;
            this.payloadFingerprint = payloadFingerprint;
            this.opaqueHandle = opaqueHandle;
            returnedTurn = turn;
            return turn;
        }
    }

    private static final class ConvergingCreateUserTurnPort implements CreateUserTurnPort {
        private AgentTurn firstCandidate;
        private AgentTurn secondCandidate;
        private Conversation firstConversation;
        private Conversation secondConversation;
        private AgentTurn canonicalTurn;

        @Override
        public AgentTurn createOrGetUserTurn(
                Conversation conversation,
                AgentTurn turn,
                AgentOwnerId ownerId,
                String idempotencyKey,
                String payloadFingerprint,
                String opaqueHandle
        ) {
            if (canonicalTurn == null) {
                firstCandidate = turn;
                firstConversation = conversation;
                canonicalTurn = turn;
                return turn;
            }
            secondCandidate = turn;
            secondConversation = conversation;
            return canonicalTurn;
        }
    }

    private static final class ConflictingCreateUserTurnPort implements CreateUserTurnPort {
        @Override
        public AgentTurn createOrGetUserTurn(
                Conversation conversation,
                AgentTurn turn,
                AgentOwnerId ownerId,
                String idempotencyKey,
                String payloadFingerprint,
                String opaqueHandle
        ) {
            throw new IdempotencyKeyReusedException();
        }
    }
}
