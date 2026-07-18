package com.ar.crm2.application.agent.turn.service;

import com.ar.crm2.application.agent.turn.command.RegenerateUserTurnCommand;
import com.ar.crm2.application.agent.turn.port.out.ChatCompletionPort;
import com.ar.crm2.application.agent.turn.port.out.CompleteRegeneratedTurnPort;
import com.ar.crm2.application.agent.turn.port.out.CreateRegenerationPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedVisibleHistoryPort;
import com.ar.crm2.application.agent.turn.port.out.FindEligibleDurableMemoriesPort;
import com.ar.crm2.application.agent.turn.port.out.FindUserTurnContentPort;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegenerateUserTurnServiceTest {

    @Test
    void allowsDirectConstructionWithoutDependencyValidation() {
        assertDoesNotThrow(() -> new RegenerateUserTurnService(null, null, null, null, null, null));
    }

    @Test
    void returnsCanonicalSameKeyRetryWithoutModelCallOrVisibleOutputWrite() {
        UUID turnId = UUID.randomUUID();
        CapturingRegenerationPort regenerationPort = new CapturingRegenerationPort(Optional.of("canonical current output"));
        CapturingRegeneratedCompletionPort completionPort = new CapturingRegeneratedCompletionPort();
        CapturingChatCompletionPort chatCompletionPort = new CapturingChatCompletionPort("provider output");
        CapturingHistoryPort historyPort = new CapturingHistoryPort(List.of("history"));
        CapturingUserContentPort userContentPort = new CapturingUserContentPort("original user content");
        CapturingMemoryPort memoryPort = new CapturingMemoryPort(List.of("memory"));
        RegenerateUserTurnService service = new RegenerateUserTurnService(
                regenerationPort,
                historyPort,
                userContentPort,
                memoryPort,
                completionPort,
                chatCompletionPort
        );

        String content = service.regenerate(new RegenerateUserTurnCommand(" owner-a ", turnId, " handle-a ", " key-a ", 10));

        assertEquals("canonical current output", content);
        assertEquals(1, regenerationPort.calls);
        assertEquals(List.of(AgentOwnerId.from("owner-a")), regenerationPort.ownerIds);
        assertEquals(List.of(TurnId.from(turnId)), regenerationPort.turnIds);
        assertEquals(List.of("handle-a"), regenerationPort.opaqueHandles);
        assertEquals(List.of("key-a"), regenerationPort.idempotencyKeys);
        assertEquals(0, historyPort.calls);
        assertEquals(0, userContentPort.calls);
        assertEquals(0, memoryPort.calls);
        assertEquals(0, chatCompletionPort.calls);
        assertEquals(0, completionPort.calls);
    }

    @Test
    void startsSequentialRegenerationsWithOriginalUserContentExactlyOnceAndConvergesEachOutput() {
        UUID turnId = UUID.randomUUID();
        CapturingRegenerationPort regenerationPort = new CapturingRegenerationPort(Optional.empty());
        CapturingRegeneratedCompletionPort completionPort = new CapturingRegeneratedCompletionPort();
        CapturingChatCompletionPort chatCompletionPort = new CapturingChatCompletionPort("first regenerated output", "second regenerated output");
        CapturingHistoryPort historyPort = new CapturingHistoryPort(List.of("earlier completed exchange"));
        CapturingUserContentPort userContentPort = new CapturingUserContentPort("original user content");
        CapturingMemoryPort memoryPort = new CapturingMemoryPort(List.of("durable instruction"));
        RegenerateUserTurnService service = new RegenerateUserTurnService(
                regenerationPort,
                historyPort,
                userContentPort,
                memoryPort,
                completionPort,
                chatCompletionPort
        );

        String first = service.regenerate(new RegenerateUserTurnCommand("owner-a", turnId, "handle-a", "key-a", 5));
        String second = service.regenerate(new RegenerateUserTurnCommand("owner-a", turnId, "handle-a", "key-b", 5));

        assertEquals("canonical first regenerated output", first);
        assertEquals("canonical second regenerated output", second);
        assertEquals(2, regenerationPort.calls);
        assertEquals(List.of(AgentOwnerId.from("owner-a"), AgentOwnerId.from("owner-a")), regenerationPort.ownerIds);
        assertEquals(List.of(TurnId.from(turnId), TurnId.from(turnId)), regenerationPort.turnIds);
        assertEquals(List.of("handle-a", "handle-a"), regenerationPort.opaqueHandles);
        assertEquals(List.of("key-a", "key-b"), regenerationPort.idempotencyKeys);
        assertEquals(2, historyPort.calls);
        assertEquals(List.of(AgentOwnerId.from("owner-a"), AgentOwnerId.from("owner-a")), historyPort.ownerIds);
        assertEquals(List.of(TurnId.from(turnId), TurnId.from(turnId)), historyPort.turnIds);
        assertEquals(List.of("handle-a", "handle-a"), historyPort.opaqueHandles);
        assertEquals(List.of(5, 5), historyPort.maximumMessages);
        assertEquals(2, userContentPort.calls);
        assertEquals(List.of(AgentOwnerId.from("owner-a"), AgentOwnerId.from("owner-a")), userContentPort.ownerIds);
        assertEquals(List.of(TurnId.from(turnId), TurnId.from(turnId)), userContentPort.turnIds);
        assertEquals(List.of("handle-a", "handle-a"), userContentPort.opaqueHandles);
        assertEquals(2, memoryPort.calls);
        assertEquals(List.of(AgentOwnerId.from("owner-a"), AgentOwnerId.from("owner-a")), memoryPort.ownerIds);
        assertEquals(2, completionPort.calls);
        assertEquals(List.of(AgentOwnerId.from("owner-a"), AgentOwnerId.from("owner-a")), completionPort.ownerIds);
        assertEquals(List.of(TurnId.from(turnId), TurnId.from(turnId)), completionPort.turnIds);
        assertEquals(List.of("handle-a", "handle-a"), completionPort.opaqueHandles);
        assertEquals(List.of("earlier completed exchange"), chatCompletionPort.visibleHistory.get(0));
        assertEquals(List.of("durable instruction"), chatCompletionPort.durableMemories.get(0));
        assertEquals(List.of("original user content", "original user content"), chatCompletionPort.prompts);
        assertEquals(0, chatCompletionPort.visibleHistory.stream()
                .flatMap(List::stream).filter("original user content"::equals).count());
        assertEquals(List.of(AgentOwnerId.from("owner-a"), AgentOwnerId.from("owner-a")), chatCompletionPort.ownerIds);
        assertEquals(List.of(TurnId.from(turnId), TurnId.from(turnId)), chatCompletionPort.turnIds);
        assertEquals(List.of("key-a", "key-b"), completionPort.idempotencyKeys);
    }

    @Test
    void doesNotWriteRegeneratedOutputWhenTheProviderFails() {
        CapturingRegeneratedCompletionPort completionPort = new CapturingRegeneratedCompletionPort();
        RegenerateUserTurnService service = new RegenerateUserTurnService(
                new CapturingRegenerationPort(Optional.empty()),
                (ownerId, turnId, opaqueHandle, maximumMessages) -> List.of("history"),
                (ownerId, turnId, opaqueHandle) -> "original user content",
                ownerId -> List.of("memory"),
                completionPort,
                (ownerId, turnId, visibleHistory, durableMemories, prompt) -> {
                    throw new IllegalStateException("provider failed");
                }
        );

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> service.regenerate(
                new RegenerateUserTurnCommand("owner-a", UUID.randomUUID(), "handle-a", "key-a", 5)));

        assertEquals("provider failed", failure.getMessage());
        assertEquals(0, completionPort.calls);
    }

    private static final class CapturingRegenerationPort implements CreateRegenerationPort {
        private final Optional<String> canonicalContent;
        private int calls;
        private final List<AgentOwnerId> ownerIds = new java.util.ArrayList<>();
        private final List<TurnId> turnIds = new java.util.ArrayList<>();
        private final List<String> opaqueHandles = new java.util.ArrayList<>();
        private final List<String> idempotencyKeys = new java.util.ArrayList<>();

        private CapturingRegenerationPort(Optional<String> canonicalContent) {
            this.canonicalContent = canonicalContent;
        }

        @Override
        public Optional<String> createRegenerationOrFindCanonical(
                AgentOwnerId ownerId,
                TurnId turnId,
                String opaqueHandle,
                String idempotencyKey
        ) {
            calls++;
            ownerIds.add(ownerId);
            turnIds.add(turnId);
            opaqueHandles.add(opaqueHandle);
            idempotencyKeys.add(idempotencyKey);
            return canonicalContent;
        }
    }

    private static final class CapturingRegeneratedCompletionPort implements CompleteRegeneratedTurnPort {
        private int calls;
        private final List<AgentOwnerId> ownerIds = new java.util.ArrayList<>();
        private final List<TurnId> turnIds = new java.util.ArrayList<>();
        private final List<String> opaqueHandles = new java.util.ArrayList<>();
        private final List<String> idempotencyKeys = new java.util.ArrayList<>();

        @Override
        public String completeRegeneratedTurn(
                AgentOwnerId ownerId,
                TurnId turnId,
                String opaqueHandle,
                String idempotencyKey,
                String assistantContent
        ) {
            calls++;
            ownerIds.add(ownerId);
            turnIds.add(turnId);
            opaqueHandles.add(opaqueHandle);
            idempotencyKeys.add(idempotencyKey);
            return "canonical " + assistantContent;
        }
    }

    private static final class CapturingChatCompletionPort implements ChatCompletionPort {
        private final List<String> outputs;
        private int calls;
        private final List<AgentOwnerId> ownerIds = new java.util.ArrayList<>();
        private final List<TurnId> turnIds = new java.util.ArrayList<>();
        private final List<List<String>> visibleHistory = new java.util.ArrayList<>();
        private final List<List<String>> durableMemories = new java.util.ArrayList<>();
        private final List<String> prompts = new java.util.ArrayList<>();

        private CapturingChatCompletionPort(String... outputs) {
            this.outputs = List.of(outputs);
        }

        @Override
        public String complete(
                AgentOwnerId ownerId,
                TurnId turnId,
                List<String> visibleHistory,
                List<String> durableMemories,
                String normalizedPrompt
        ) {
            ownerIds.add(ownerId);
            turnIds.add(turnId);
            this.visibleHistory.add(visibleHistory);
            this.durableMemories.add(durableMemories);
            prompts.add(normalizedPrompt);
            return outputs.get(calls++);
        }
    }

    private static final class CapturingHistoryPort implements FindCompletedVisibleHistoryPort {
        private final List<String> history;
        private int calls;
        private final List<AgentOwnerId> ownerIds = new java.util.ArrayList<>();
        private final List<TurnId> turnIds = new java.util.ArrayList<>();
        private final List<String> opaqueHandles = new java.util.ArrayList<>();
        private final List<Integer> maximumMessages = new java.util.ArrayList<>();

        private CapturingHistoryPort(List<String> history) {
            this.history = history;
        }

        @Override
        public List<String> findCompletedVisibleHistory(
                AgentOwnerId ownerId,
                TurnId turnId,
                String opaqueHandle,
                int maximumMessages
        ) {
            calls++;
            ownerIds.add(ownerId);
            turnIds.add(turnId);
            opaqueHandles.add(opaqueHandle);
            this.maximumMessages.add(maximumMessages);
            return history;
        }
    }

    private static final class CapturingUserContentPort implements FindUserTurnContentPort {
        private final String userContent;
        private int calls;
        private final List<AgentOwnerId> ownerIds = new java.util.ArrayList<>();
        private final List<TurnId> turnIds = new java.util.ArrayList<>();
        private final List<String> opaqueHandles = new java.util.ArrayList<>();

        private CapturingUserContentPort(String userContent) {
            this.userContent = userContent;
        }

        @Override
        public String findUserTurnContent(AgentOwnerId ownerId, TurnId turnId, String opaqueHandle) {
            calls++;
            ownerIds.add(ownerId);
            turnIds.add(turnId);
            opaqueHandles.add(opaqueHandle);
            return userContent;
        }
    }

    private static final class CapturingMemoryPort implements FindEligibleDurableMemoriesPort {
        private final List<String> memories;
        private int calls;
        private final List<AgentOwnerId> ownerIds = new java.util.ArrayList<>();

        private CapturingMemoryPort(List<String> memories) {
            this.memories = memories;
        }

        @Override
        public List<String> findEligibleDurableMemories(AgentOwnerId ownerId) {
            calls++;
            ownerIds.add(ownerId);
            return memories;
        }
    }
}
