package com.ar.crm2.application.agent.turn.service;

import com.ar.crm2.application.agent.turn.command.CompleteUserTurnCommand;
import com.ar.crm2.application.agent.turn.port.out.ChatCompletionPort;
import com.ar.crm2.application.agent.turn.port.out.CompletePreparedTurnPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedAssistantContentPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedVisibleHistoryPort;
import com.ar.crm2.application.agent.turn.port.out.FindEligibleDurableMemoriesPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import com.ar.crm2.application.support.TestCrmAuthorization;
import com.ar.crm2.application.support.TestCurrentActorPort;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.agent.vo.VisibleMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class CompleteUserTurnServiceTest {

    private static final UUID ACTOR_USUARIO_ID =
            UUID.fromString("aaaaaaaa-1111-2222-3333-444444444444");
    private static final UUID ACTOR_SUPER_USUARIO_ID =
            UUID.fromString("bbbbbbbb-1111-2222-3333-444444444444");

    @Test
    void allowsDirectConstructionWithoutDependencyNullValidation() {
        assertDoesNotThrow(() -> new CompleteUserTurnService(null, null, null, null, null, null, null));
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
                chatCompletionPort,
                new TestCrmAuthorization(),
                new TestCurrentActorPort(ACTOR_USUARIO_ID)
        );

        String content = service.complete(new CompleteUserTurnCommand(
                " owner-a ", ACTOR_USUARIO_ID, turnId, " handle-a ", " prompt ", 12));

        assertEquals("canonical persisted output", content);
        assertEquals(1, completedContentPort.calls);
        assertEquals(AgentOwnerId.from("owner-a"), completedContentPort.ownerId);
        assertEquals(TurnId.from(turnId), completedContentPort.turnId);
        assertEquals("handle-a", completedContentPort.opaqueHandle);
        assertEquals("test-authorization-revision", completedContentPort.authorizationRevision);
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
                chatCompletionPort,
                new TestCrmAuthorization(),
                new TestCurrentActorPort(ACTOR_USUARIO_ID)
        );

        String content = service.complete(new CompleteUserTurnCommand(
                "owner-a", ACTOR_USUARIO_ID, ACTOR_SUPER_USUARIO_ID,
                turnId, "handle-a", "  current prompt  ", 7));

        assertEquals("canonical converged output", content);
        assertEquals(1, chatCompletionPort.calls);
        assertEquals(AgentOwnerId.from("owner-a"), chatCompletionPort.ownerId);
        assertEquals(ACTOR_USUARIO_ID, chatCompletionPort.actorUsuarioId);
        assertEquals(ACTOR_SUPER_USUARIO_ID, chatCompletionPort.actorSuperUsuarioId);
        assertEquals(TurnId.from(turnId), chatCompletionPort.turnId);
        assertEquals(orderedHistory, chatCompletionPort.visibleHistory);
        assertEquals(List.of("remember the customer timezone"), chatCompletionPort.durableMemories);
        assertEquals("current prompt", chatCompletionPort.prompt);
        assertEquals(AgentOwnerId.from("owner-a"), historyPort.ownerId);
        assertEquals(TurnId.from(turnId), historyPort.turnId);
        assertEquals("handle-a", historyPort.opaqueHandle);
        assertEquals(7, historyPort.maximumMessages);
        assertEquals(AgentOwnerId.from("owner-a"), memoryPort.ownerId);
        assertEquals("test-authorization-revision", historyPort.authorizationRevision);
        assertEquals("test-authorization-revision", memoryPort.authorizationRevision);
        assertEquals(1, completionPort.calls);
        assertEquals("provider output", completionPort.assistantContent);
        assertEquals(AgentOwnerId.from("owner-a"), completionPort.ownerId);
        assertEquals(TurnId.from(turnId), completionPort.turnId);
        assertEquals("handle-a", completionPort.opaqueHandle);
        assertEquals("test-authorization-revision", completionPort.authorizationRevision);
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
                (ownerId, turn, handle, revision) -> Optional.empty(),
                historyPort,
                (ownerId, revision) -> List.of(),
                (owner, turn, handle, revision, content) -> content,
                chatCompletionPort,
                new TestCrmAuthorization(),
                new TestCurrentActorPort(ACTOR_USUARIO_ID)
        );

        service.complete(new CompleteUserTurnCommand(
                "owner-a", ACTOR_USUARIO_ID, turnId, "handle-a", "prompt", 10));

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
                (ownerId, turn, handle, revision) -> Optional.empty(),
                historyPort,
                memoryPort,
                (owner, turn, handle, revision, content) -> content,
                chatCompletionPort,
                new TestCrmAuthorization(),
                new TestCurrentActorPort(ACTOR_USUARIO_ID)
        );

        service.complete(new CompleteUserTurnCommand(
                "owner-a", ACTOR_USUARIO_ID, turnId, "handle-a", "prompt", 5));

        assertEquals(userOnlyHistory, chatCompletionPort.visibleHistory);
        for (VisibleMessage message : chatCompletionPort.visibleHistory) {
            assertEquals(com.ar.crm2.model.agent.enums.VisibleMessageRole.USER, message.role());
        }
    }

    @Test
    void doesNotAttemptCompletionPersistenceWhenTheModelFails() {
        CapturingCompletionPort completionPort = new CapturingCompletionPort("unused");
        CompleteUserTurnService service = new CompleteUserTurnService(
                (ownerId, turnId, opaqueHandle, revision) -> Optional.empty(),
                (ownerId, turnId, opaqueHandle, maximumMessages, revision) -> List.of(VisibleMessage.user("history")),
                (ownerId, revision) -> List.of("memory"),
                completionPort,
                (ownerId, actorUsuarioId, actorSuperUsuarioId, turnId, visibleHistory, durableMemories, prompt) -> {
                    throw new IllegalStateException("provider failed");
                },
                new TestCrmAuthorization(),
                new TestCurrentActorPort(ACTOR_USUARIO_ID)
        );

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> service.complete(
                new CompleteUserTurnCommand(
                        "owner-a", ACTOR_USUARIO_ID, UUID.randomUUID(), "handle-a", "prompt", 3)));

        assertEquals("provider failed", failure.getMessage());
        assertEquals(0, completionPort.calls);
    }

    @Test
    void forwardsActorUsuarioIdToCompletionIndependentOfOwnerSubjectValue() {
        UUID turnId = UUID.randomUUID();
        UUID distinctActorUsuarioId =
                UUID.fromString("99999999-8888-7777-6666-555555555555");
        CapturingChatCompletionPort chatCompletionPort = new CapturingChatCompletionPort("provider output");
        CompleteUserTurnService service = new CompleteUserTurnService(
                (ownerId, turn, handle, revision) -> Optional.empty(),
                (ownerId, turn, handle, max, revision) -> List.of(),
                (ownerId, revision) -> List.of(),
                (owner, turn, handle, revision, content) -> content,
                chatCompletionPort,
                new TestCrmAuthorization(),
                new TestCurrentActorPort(ACTOR_USUARIO_ID)
        );

        service.complete(new CompleteUserTurnCommand(
                "owner-subject-not-crm-id", distinctActorUsuarioId, turnId, "handle-a", "prompt", 5));

        assertEquals(AgentOwnerId.from("owner-subject-not-crm-id"), chatCompletionPort.ownerId);
        assertEquals(ACTOR_USUARIO_ID, chatCompletionPort.actorUsuarioId,
                "The active CRM actor must override a command-supplied actor UUID");
        assertEquals("owner-subject-not-crm-id", AgentOwnerId.from("owner-subject-not-crm-id").value());
    }

    @Test
    void forwardsActorUsuarioIdUnchangedEvenWhenOwnerSubjectIsIgnored() {
        UUID turnId = UUID.randomUUID();
        UUID repeatedActor =
                UUID.fromString("cafebabe-0000-0000-0000-000000000000");
        CapturingChatCompletionPort chatCompletionPort = new CapturingChatCompletionPort("provider output");
        CompleteUserTurnService service = new CompleteUserTurnService(
                (ownerId, turn, handle, revision) -> Optional.empty(),
                (ownerId, turn, handle, max, revision) -> List.of(),
                (ownerId, revision) -> List.of(),
                (owner, turn, handle, revision, content) -> content,
                chatCompletionPort,
                new TestCrmAuthorization(),
                new TestCurrentActorPort(ACTOR_USUARIO_ID)
        );

        service.complete(new CompleteUserTurnCommand(
                "different-owner", repeatedActor, turnId, "handle", "prompt", 5));

        assertEquals(ACTOR_USUARIO_ID, chatCompletionPort.actorUsuarioId,
                "The active CRM actor must override a command-supplied actor UUID");
        assertEquals(AgentOwnerId.from("different-owner"), chatCompletionPort.ownerId);
    }

    @Test
    void dropsCompletionWhenAuthorizationRevisionChangesDuringModelCall() {
        AtomicReference<String> revisionState = new AtomicReference<>("revision-before-model");
        TestCrmAuthorization changingAuthorization = new TestCrmAuthorization() {
            @Override
            public String revision() {
                return revisionState.get();
            }
        };
        CapturingCompletionPort completionPort = new CapturingCompletionPort("unused");
        CompleteUserTurnService service = new CompleteUserTurnService(
                (owner, turn, handle, revision) -> Optional.empty(),
                (owner, turn, handle, maximum, revision) -> List.of(),
                (owner, revision) -> List.of(),
                completionPort,
                (owner, actor, superActor, turn, history, memories, prompt) -> {
                    revisionState.set("revision-after-model");
                    return "model output";
                },
                changingAuthorization,
                new TestCurrentActorPort(ACTOR_USUARIO_ID)
        );

        assertThrows(CrmActorUnavailableException.class, () -> service.complete(new CompleteUserTurnCommand(
                "owner-a", UUID.randomUUID(), UUID.randomUUID(), "handle", "prompt", 5)));
        assertEquals(0, completionPort.calls,
                "A response generated under a stale authorization revision must not be persisted");
    }

    @Test
    void doesNotCallProviderWhenAuthorizationChangesWhileLoadingHistory() {
        AtomicReference<String> revision = new AtomicReference<>("revision-before-context");
        CrmAuthorization authorization = new TestCrmAuthorization() {
            @Override
            public String revision() {
                return revision.get();
            }
        };
        ChatCompletionPort provider = mock(ChatCompletionPort.class);
        CapturingCompletionPort completionPort = new CapturingCompletionPort("unused");
        CompleteUserTurnService service = new CompleteUserTurnService(
                (owner, turn, handle, authorizationRevision) -> Optional.empty(),
                (owner, turn, handle, maximum, authorizationRevision) -> {
                    revision.set("revision-after-context");
                    return List.of(VisibleMessage.user("history"));
                },
                (owner, authorizationRevision) -> List.of("memory"),
                completionPort,
                provider,
                authorization,
                new TestCurrentActorPort(ACTOR_USUARIO_ID)
        );

        assertThrows(CrmActorUnavailableException.class, () -> service.complete(new CompleteUserTurnCommand(
                "owner-a", ACTOR_USUARIO_ID, UUID.randomUUID(), "handle", "prompt", 5)));
        verifyNoInteractions(provider);
        assertEquals(0, completionPort.calls);
    }

    @Test
    void rejectsCanonicalRetryWhenAuthorizationChangesDuringLookup() {
        AtomicReference<String> revision = new AtomicReference<>("revision-before-lookup");
        CrmAuthorization authorization = new TestCrmAuthorization() {
            @Override
            public String revision() {
                return revision.get();
            }
        };
        ChatCompletionPort provider = mock(ChatCompletionPort.class);
        CompleteUserTurnService service = new CompleteUserTurnService(
                (owner, turn, handle, authorizationRevision) -> {
                    revision.set("revision-after-lookup");
                    return Optional.of("stale cached output");
                },
                null, null, null, provider, authorization,
                new TestCurrentActorPort(ACTOR_USUARIO_ID));

        assertThrows(CrmActorUnavailableException.class, () -> service.complete(new CompleteUserTurnCommand(
                "owner-a", ACTOR_USUARIO_ID, UUID.randomUUID(), "handle", "prompt", 5)));
        verifyNoInteractions(provider);
    }

    private static final class CapturingHistoryPort implements FindCompletedVisibleHistoryPort {
        private final List<VisibleMessage> history;
        private int calls;
        private AgentOwnerId ownerId;
        private TurnId turnId;
        private String opaqueHandle;
        private int maximumMessages;
        private String authorizationRevision;

        private CapturingHistoryPort(List<VisibleMessage> history) {
            this.history = history;
        }

        @Override
        public List<VisibleMessage> findCompletedVisibleHistory(
                AgentOwnerId ownerId,
                TurnId turnId,
                String opaqueHandle,
                int maximumMessages,
                String authorizationRevision
        ) {
            calls++;
            this.ownerId = ownerId;
            this.turnId = turnId;
            this.opaqueHandle = opaqueHandle;
            this.maximumMessages = maximumMessages;
            this.authorizationRevision = authorizationRevision;
            return history;
        }
    }

    private static final class CapturingMemoryPort implements FindEligibleDurableMemoriesPort {
        private final List<String> memories;
        private int calls;
        private AgentOwnerId ownerId;
        private String authorizationRevision;

        private CapturingMemoryPort(List<String> memories) {
            this.memories = memories;
        }

        @Override
        public List<String> findEligibleDurableMemories(AgentOwnerId ownerId, String authorizationRevision) {
            calls++;
            this.ownerId = ownerId;
            this.authorizationRevision = authorizationRevision;
            return memories;
        }
    }

    private static final class CapturingCompletedContentPort implements FindCompletedAssistantContentPort {
        private final Optional<String> content;
        private int calls;
        private AgentOwnerId ownerId;
        private TurnId turnId;
        private String opaqueHandle;
        private String authorizationRevision;

        private CapturingCompletedContentPort(Optional<String> content) {
            this.content = content;
        }

        @Override
        public Optional<String> findCompletedAssistantContent(
                AgentOwnerId ownerId,
                TurnId turnId,
                String opaqueHandle,
                String authorizationRevision
        ) {
            calls++;
            this.ownerId = ownerId;
            this.turnId = turnId;
            this.opaqueHandle = opaqueHandle;
            this.authorizationRevision = authorizationRevision;
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
        private String authorizationRevision;

        private CapturingCompletionPort(String canonicalContent) {
            this.canonicalContent = canonicalContent;
        }

        @Override
        public String completePreparedTurn(
                AgentOwnerId ownerId,
                TurnId turnId,
                String opaqueHandle,
                String authorizationRevision,
                String assistantContent
        ) {
            calls++;
            this.ownerId = ownerId;
            this.turnId = turnId;
            this.opaqueHandle = opaqueHandle;
            this.authorizationRevision = authorizationRevision;
            this.assistantContent = assistantContent;
            return canonicalContent;
        }
    }

    private static final class CapturingChatCompletionPort implements ChatCompletionPort {
        private final String output;
        private int calls;
        private AgentOwnerId ownerId;
        private UUID actorUsuarioId;
        private UUID actorSuperUsuarioId;
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
                UUID actorUsuarioId,
                UUID actorSuperUsuarioId,
                TurnId turnId,
                List<VisibleMessage> visibleHistory,
                List<String> durableMemories,
                String normalizedPrompt
        ) {
            calls++;
            this.ownerId = ownerId;
            this.actorUsuarioId = actorUsuarioId;
            this.actorSuperUsuarioId = actorSuperUsuarioId;
            this.turnId = turnId;
            this.visibleHistory = visibleHistory;
            this.durableMemories = durableMemories;
            prompt = normalizedPrompt;
            return output;
        }
    }
}
