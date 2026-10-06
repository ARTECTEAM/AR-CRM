package com.ar.crm2.application.agent.turn.service;

import com.ar.crm2.application.agent.turn.command.CompleteUserTurnCommand;
import com.ar.crm2.application.agent.turn.port.out.ChatCompletionPort;
import com.ar.crm2.application.agent.turn.port.out.CompletePreparedTurnPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedAssistantContentPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedVisibleHistoryPort;
import com.ar.crm2.application.agent.turn.port.out.FindEligibleDurableMemoriesPort;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.agent.vo.VisibleMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompleteUserTurnServiceTest {

    @Test
    void allowsDirectConstructionWithoutDependencyNullValidation() {
        assertDoesNotThrow(() -> new CompleteUserTurnService(null, null, null, null, null));
    }

    @Test
    void returnsCanonicalCompletedContentWithoutCallingTheModel() {
        UUID turnId = UUID.randomUUID();
        CapturingCompletedContentPort completedContentPort = new CapturingCompletedContentPort(
                Optional.of("canonical persisted output"));
        CapturingHistoryPort historyPort = new CapturingHistoryPort(List.of());
        CapturingMemoryPort memoryPort = new CapturingMemoryPort(List.of("must not load memories"));
        CapturingCompletionPort completionPort = new CapturingCompletionPort("unused");
        CapturingChatCompletionPort chatCompletionPort = new CapturingChatCompletionPort("model output");
        CompleteUserTurnService service = new CompleteUserTurnService(
                completedContentPort,
                historyPort,
                memoryPort,
                completionPort,
                chatCompletionPort
        );

        String content = service.complete(new CompleteUserTurnCommand(
                " owner-a ", turnId, " handle-a ", " prompt ", 12));

        assertEquals("canonical persisted output", content);
        assertEquals(1, completedContentPort.calls);
        assertEquals(AgentOwnerId.from("owner-a"), completedContentPort.ownerId);
        assertEquals(TurnId.from(turnId), completedContentPort.turnId);
        assertEquals("handle-a", completedContentPort.opaqueHandle);
        assertEquals(0, historyPort.calls);
        assertEquals(0, memoryPort.calls);
        assertEquals(0, chatCompletionPort.calls);
        assertEquals(0, completionPort.calls);
    }

    @Test
    void completesPreparedTurnWithSeparateBoundedRoleBearingHistoryAndDurableMemoryContext() {
        UUID turnId = UUID.randomUUID();
        List<VisibleMessage> orderedHistory = List.of(
                VisibleMessage.user("prior user question"),
                VisibleMessage.assistant("prior assistant reply")
        );
        CapturingCompletionPort completionPort = new CapturingCompletionPort("canonical converged output");
        CapturingChatCompletionPort chatCompletionPort = new CapturingChatCompletionPort("provider output");
        CapturingHistoryPort historyPort = new CapturingHistoryPort(orderedHistory);
        CapturingMemoryPort memoryPort = new CapturingMemoryPort(List.of("remember the customer timezone"));
        CapturingCompletedContentPort completedContentPort = new CapturingCompletedContentPort(Optional.empty());
        CompleteUserTurnService service = new CompleteUserTurnService(
                completedContentPort,
                historyPort,
                memoryPort,
                completionPort,
                chatCompletionPort
        );

        String content = service.complete(new CompleteUserTurnCommand(
                "owner-a", turnId, "handle-a", "  current prompt  ", 7));

        assertEquals("canonical converged output", content);
        assertEquals(1, chatCompletionPort.calls);
        assertEquals(AgentOwnerId.from("owner-a"), chatCompletionPort.ownerId);
        assertEquals(TurnId.from(turnId), chatCompletionPort.turnId);
        assertEquals(orderedHistory, chatCompletionPort.visibleHistory);
        assertEquals(List.of("remember the customer timezone"), chatCompletionPort.durableMemories);
        assertEquals("current prompt", chatCompletionPort.prompt);
        assertEquals(AgentOwnerId.from("owner-a"), historyPort.ownerId);
        assertEquals(TurnId.from(turnId), historyPort.turnId);
        assertEquals("handle-a", historyPort.opaqueHandle);
        assertEquals(7, historyPort.maximumMessages);
        assertEquals(AgentOwnerId.from("owner-a"), memoryPort.ownerId);
        assertEquals(1, completionPort.calls);
        assertEquals("provider output", completionPort.assistantContent);
        assertEquals(AgentOwnerId.from("owner-a"), completionPort.ownerId);
        assertEquals(TurnId.from(turnId), completionPort.turnId);
        assertEquals("handle-a", completionPort.opaqueHandle);
    }

    @Test
    void preservesRoleProvenanceAndOrderingWhenPassingVisibleHistoryToTheModel() {
        UUID turnId = UUID.randomUUID();
        List<VisibleMessage> orderedHistory = new ArrayList<>();
        orderedHistory.add(VisibleMessage.user("first user"));
        orderedHistory.add(VisibleMessage.assistant("first assistant"));
        orderedHistory.add(VisibleMessage.user("second user"));
        orderedHistory.add(VisibleMessage.assistant("second assistant"));
        CapturingHistoryPort historyPort = new CapturingHistoryPort(orderedHistory);
        CapturingChatCompletionPort chatCompletionPort = new CapturingChatCompletionPort("provider output");
        CompleteUserTurnService service = new CompleteUserTurnService(
                (ownerId, turn, handle) -> Optional.empty(),
                historyPort,
                ownerId -> List.of(),
                (owner, turn, handle, content) -> content,
                chatCompletionPort
        );

        service.complete(new CompleteUserTurnCommand(
                "owner-a", turnId, "handle-a", "prompt", 10));

        List<VisibleMessage> delivered = new ArrayList<>(chatCompletionPort.visibleHistory);
        assertEquals(orderedHistory, delivered);
        assertEquals(com.ar.crm2.model.agent.enums.VisibleMessageRole.USER, delivered.get(0).role());
        assertEquals(com.ar.crm2.model.agent.enums.VisibleMessageRole.ASSISTANT, delivered.get(1).role());
        assertEquals(com.ar.crm2.model.agent.enums.VisibleMessageRole.USER, delivered.get(2).role());
        assertEquals(com.ar.crm2.model.agent.enums.VisibleMessageRole.ASSISTANT, delivered.get(3).role());
    }

    @Test
    void visibleHistoryRolesAreSpeakerProvenanceOnlyAndNotAuthorizationSignals() {
        UUID turnId = UUID.randomUUID();
        List<VisibleMessage> userOnlyHistory = List.of(
                VisibleMessage.user("user turn 1"),
                VisibleMessage.user("user turn 2")
        );
        CapturingHistoryPort historyPort = new CapturingHistoryPort(userOnlyHistory);
        CapturingChatCompletionPort chatCompletionPort = new CapturingChatCompletionPort("provider output");
        CapturingMemoryPort memoryPort = new CapturingMemoryPort(List.of());
        CompleteUserTurnService service = new CompleteUserTurnService(
                (ownerId, turn, handle) -> Optional.empty(),
                historyPort,
                memoryPort,
                (owner, turn, handle, content) -> content,
                chatCompletionPort
        );

        service.complete(new CompleteUserTurnCommand(
                "owner-a", turnId, "handle-a", "prompt", 5));

        assertEquals(userOnlyHistory, chatCompletionPort.visibleHistory);
        for (VisibleMessage message : chatCompletionPort.visibleHistory) {
            assertEquals(com.ar.crm2.model.agent.enums.VisibleMessageRole.USER, message.role());
        }
    }

    @Test
    void doesNotAttemptCompletionPersistenceWhenTheModelFails() {
        CapturingCompletionPort completionPort = new CapturingCompletionPort("unused");
        CompleteUserTurnService service = new CompleteUserTurnService(
                (ownerId, turnId, opaqueHandle) -> Optional.empty(),
                (ownerId, turnId, opaqueHandle, maximumMessages) -> List.of(VisibleMessage.user("history")),
                ownerId -> List.of("memory"),
                completionPort,
                (ownerId, turnId, visibleHistory, durableMemories, prompt) -> {
                    throw new IllegalStateException("provider failed");
                }
        );

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> service.complete(
                new CompleteUserTurnCommand("owner-a", UUID.randomUUID(), "handle-a", "prompt", 3)));

        assertEquals("provider failed", failure.getMessage());
        assertEquals(0, completionPort.calls);
    }

    private static final class CapturingHistoryPort implements FindCompletedVisibleHistoryPort {
        private final List<VisibleMessage> history;
        private int calls;
        private AgentOwnerId ownerId;
        private TurnId turnId;
        private String opaqueHandle;
        private int maximumMessages;

        private CapturingHistoryPort(List<VisibleMessage> history) {
            this.history = history;
        }

        @Override
        public List<VisibleMessage> findCompletedVisibleHistory(
                AgentOwnerId ownerId,
                TurnId turnId,
                String opaqueHandle,
                int maximumMessages
        ) {
            calls++;
            this.ownerId = ownerId;
            this.turnId = turnId;
            this.opaqueHandle = opaqueHandle;
            this.maximumMessages = maximumMessages;
            return history;
        }
    }

    private static final class CapturingMemoryPort implements FindEligibleDurableMemoriesPort {
        private final List<String> memories;
        private int calls;
        private AgentOwnerId ownerId;

        private CapturingMemoryPort(List<String> memories) {
            this.memories = memories;
        }

        @Override
        public List<String> findEligibleDurableMemories(AgentOwnerId ownerId) {
            calls++;
            this.ownerId = ownerId;
            return memories;
        }
    }

    private static final class CapturingCompletedContentPort implements FindCompletedAssistantContentPort {
        private final Optional<String> content;
        private int calls;
        private AgentOwnerId ownerId;
        private TurnId turnId;
        private String opaqueHandle;

        private CapturingCompletedContentPort(Optional<String> content) {
            this.content = content;
        }

        @Override
        public Optional<String> findCompletedAssistantContent(
                AgentOwnerId ownerId,
                TurnId turnId,
                String opaqueHandle
        ) {
            calls++;
            this.ownerId = ownerId;
            this.turnId = turnId;
            this.opaqueHandle = opaqueHandle;
            return content;
        }
    }

    private static final class CapturingCompletionPort implements CompletePreparedTurnPort {
        private final String canonicalContent;
        private int calls;
        private AgentOwnerId ownerId;
        private TurnId turnId;
        private String opaqueHandle;
        private String assistantContent;

        private CapturingCompletionPort(String canonicalContent) {
            this.canonicalContent = canonicalContent;
        }

        @Override
        public String completePreparedTurn(
                AgentOwnerId ownerId,
                TurnId turnId,
                String opaqueHandle,
                String assistantContent
        ) {
            calls++;
            this.ownerId = ownerId;
            this.turnId = turnId;
            this.opaqueHandle = opaqueHandle;
            this.assistantContent = assistantContent;
            return canonicalContent;
        }
    }

    private static final class CapturingChatCompletionPort implements ChatCompletionPort {
        private final String output;
        private int calls;
        private AgentOwnerId ownerId;
        private TurnId turnId;
        private List<VisibleMessage> visibleHistory;
        private List<String> durableMemories;
        private String prompt;

        private CapturingChatCompletionPort(String output) {
            this.output = output;
        }

        @Override
        public String complete(
                AgentOwnerId ownerId,
                TurnId turnId,
                List<VisibleMessage> visibleHistory,
                List<String> durableMemories,
                String normalizedPrompt
        ) {
            calls++;
            this.ownerId = ownerId;
            this.turnId = turnId;
            this.visibleHistory = visibleHistory;
            this.durableMemories = durableMemories;
            prompt = normalizedPrompt;
            return output;
        }
    }
}
