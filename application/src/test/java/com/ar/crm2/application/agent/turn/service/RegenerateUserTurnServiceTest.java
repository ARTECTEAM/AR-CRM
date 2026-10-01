package com.ar.crm2.application.agent.turn.service;

import com.ar.crm2.application.agent.turn.command.RegenerateUserTurnCommand;
import com.ar.crm2.application.agent.turn.port.out.ChatCompletionPort;
import com.ar.crm2.application.agent.turn.port.out.CompleteRegeneratedTurnPort;
import com.ar.crm2.application.agent.turn.port.out.CreateRegenerationPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedVisibleHistoryPort;
import com.ar.crm2.application.agent.turn.port.out.FindEligibleDurableMemoriesPort;
import com.ar.crm2.application.agent.turn.port.out.FindUserTurnContentPort;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class RegenerateUserTurnServiceTest {

    private static final UUID ACTOR_USUARIO_ID =
            UUID.fromString("bbbbbbbb-1111-2222-3333-444444444444");

    @Test
    void allowsDirectConstructionWithoutDependencyValidation() {
        assertDoesNotThrow(() -> new RegenerateUserTurnService(null, null, null, null, null, null, null, null));
    }

    @Test
    void returnsCanonicalSameKeyRetryWithoutModelCallOrVisibleOutputWrite() {
        UUID turnId = UUID.randomUUID();
        CapturingRegenerationPort regenerationPort = new CapturingRegenerationPort(Optional.of("canonical current output"));
        CapturingRegeneratedCompletionPort completionPort = new CapturingRegeneratedCompletionPort();
        CapturingChatCompletionPort chatCompletionPort = new CapturingChatCompletionPort("provider output");
        CapturingHistoryPort historyPort = new CapturingHistoryPort(List.of(VisibleMessage.user("history")));
        CapturingUserContentPort userContentPort = new CapturingUserContentPort("original user content");
        CapturingMemoryPort memoryPort = new CapturingMemoryPort(List.of("memory"));
        RegenerateUserTurnService service = new RegenerateUserTurnService(
                regenerationPort,
                historyPort,
                userContentPort,
                memoryPort,
                completionPort,
                chatCompletionPort,
                new TestCrmAuthorization(),
                new TestCurrentActorPort(ACTOR_USUARIO_ID)
        );

        String content = service.regenerate(new RegenerateUserTurnCommand(
                " owner-a ", ACTOR_USUARIO_ID, turnId, " handle-a ", " key-a ", 10));

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
        List<VisibleMessage> orderedHistory = List.of(
                VisibleMessage.user("earlier completed exchange")
        );
        CapturingRegenerationPort regenerationPort = new CapturingRegenerationPort(Optional.empty());
        CapturingRegeneratedCompletionPort completionPort = new CapturingRegeneratedCompletionPort();
        CapturingChatCompletionPort chatCompletionPort = new CapturingChatCompletionPort("first regenerated output", "second regenerated output");
        CapturingHistoryPort historyPort = new CapturingHistoryPort(orderedHistory);
        CapturingUserContentPort userContentPort = new CapturingUserContentPort("original user content");
        CapturingMemoryPort memoryPort = new CapturingMemoryPort(List.of("durable instruction"));
        RegenerateUserTurnService service = new RegenerateUserTurnService(
                regenerationPort,
                historyPort,
                userContentPort,
                memoryPort,
                completionPort,
                chatCompletionPort,
                new TestCrmAuthorization(),
                new TestCurrentActorPort(ACTOR_USUARIO_ID)
        );

        String first = service.regenerate(new RegenerateUserTurnCommand(
                "owner-a", UUID.randomUUID(), turnId, "handle-a", "key-a", 5));
        String second = service.regenerate(new RegenerateUserTurnCommand(
                "owner-a", UUID.randomUUID(), turnId, "handle-a", "key-b", 5));

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
        assertEquals(orderedHistory, chatCompletionPort.visibleHistory.get(0));
        assertEquals(List.of("durable instruction"), chatCompletionPort.durableMemories.get(0));
        assertEquals(List.of("original user content", "original user content"), chatCompletionPort.prompts);
        assertEquals(0, chatCompletionPort.visibleHistory.stream()
                .flatMap(List::stream)
                .filter(message -> "original user content".equals(message.content()))
                .count());
        assertEquals(List.of(AgentOwnerId.from("owner-a"), AgentOwnerId.from("owner-a")), chatCompletionPort.ownerIds);
        assertEquals(List.of(ACTOR_USUARIO_ID, ACTOR_USUARIO_ID), chatCompletionPort.actorUsuarioIds);
        assertEquals(List.of(TurnId.from(turnId), TurnId.from(turnId)), chatCompletionPort.turnIds);
        assertEquals(List.of("key-a", "key-b"), completionPort.idempotencyKeys);
        assertEquals(List.of("test-authorization-revision", "test-authorization-revision"),
                regenerationPort.authorizationRevisions);
        assertEquals(List.of("test-authorization-revision", "test-authorization-revision"),
                historyPort.authorizationRevisions);
        assertEquals(List.of("test-authorization-revision", "test-authorization-revision"),
                userContentPort.authorizationRevisions);
        assertEquals(List.of("test-authorization-revision", "test-authorization-revision"),
                memoryPort.authorizationRevisions);
        assertEquals(List.of("test-authorization-revision", "test-authorization-revision"),
                completionPort.authorizationRevisions);
    }

    @Test
    void doesNotWriteRegeneratedOutputWhenTheProviderFails() {
        CapturingRegeneratedCompletionPort completionPort = new CapturingRegeneratedCompletionPort();
        RegenerateUserTurnService service = new RegenerateUserTurnService(
                new CapturingRegenerationPort(Optional.empty()),
                (ownerId, turnId, opaqueHandle, maximumMessages, revision) -> List.of(VisibleMessage.user("history")),
                (ownerId, turnId, opaqueHandle, revision) -> "original user content",
                (ownerId, revision) -> List.of("memory"),
                completionPort,
                (ownerId, actorUsuarioId, actorSuperUsuarioId, turnId, visibleHistory, durableMemories, prompt) -> {
                    throw new IllegalStateException("provider failed");
                },
                new TestCrmAuthorization(),
                new TestCurrentActorPort(ACTOR_USUARIO_ID)
        );

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> service.regenerate(
                new RegenerateUserTurnCommand(
                        "owner-a", ACTOR_USUARIO_ID, UUID.randomUUID(), "handle-a", "key-a", 5)));

        assertEquals("provider failed", failure.getMessage());
        assertEquals(0, completionPort.calls);
    }

    @Test
    void missingActiveCrmActorFailsBeforeReadingOrPersistingTurnContext() {
        CapturingRegenerationPort regenerationPort = new CapturingRegenerationPort(Optional.empty());
        CapturingRegeneratedCompletionPort completionPort = new CapturingRegeneratedCompletionPort();
        RegenerateUserTurnService service = new RegenerateUserTurnService(
                regenerationPort,
                (ownerId, turnId, opaqueHandle, maximumMessages, revision) -> List.of(),
                (ownerId, turnId, opaqueHandle, revision) -> "prompt",
                (ownerId, revision) -> List.of(),
                completionPort,
                new CapturingChatCompletionPort("unused"),
                new TestCrmAuthorization(),
                Optional::empty
        );

        assertThrows(CrmActorUnavailableException.class, () -> service.regenerate(new RegenerateUserTurnCommand(
                "owner-a", UUID.randomUUID(), UUID.randomUUID(), "handle", "key", 5)));
        assertEquals(0, regenerationPort.calls);
        assertEquals(0, completionPort.calls);
    }

    @Test
    void doesNotCallProviderWhenAuthorizationChangesWhileLoadingRegenerationContext() {
        AtomicReference<String> revision = new AtomicReference<>("revision-before-context");
        CrmAuthorization authorization = new TestCrmAuthorization() {
            @Override
            public String revision() {
                return revision.get();
            }
        };
        ChatCompletionPort provider = mock(ChatCompletionPort.class);
        CapturingRegeneratedCompletionPort completionPort = new CapturingRegeneratedCompletionPort();
        RegenerateUserTurnService service = new RegenerateUserTurnService(
                (owner, turn, handle, key, authorizationRevision) -> Optional.empty(),
                (owner, turn, handle, maximum, authorizationRevision) -> {
                    revision.set("revision-after-context");
                    return List.of(VisibleMessage.user("history"));
                },
                (owner, turn, handle, authorizationRevision) -> "original user content",
                (owner, authorizationRevision) -> List.of("memory"),
                completionPort,
                provider,
                authorization,
                new TestCurrentActorPort(ACTOR_USUARIO_ID)
        );

        assertThrows(CrmActorUnavailableException.class, () -> service.regenerate(new RegenerateUserTurnCommand(
                "owner-a", ACTOR_USUARIO_ID, UUID.randomUUID(), "handle", "key", 5)));
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
        RegenerateUserTurnService service = new RegenerateUserTurnService(
                (owner, turn, handle, key, authorizationRevision) -> {
                    revision.set("revision-after-lookup");
                    return Optional.of("stale cached output");
                }, null, null, null, null, provider, authorization,
                new TestCurrentActorPort(ACTOR_USUARIO_ID));

        assertThrows(CrmActorUnavailableException.class, () -> service.regenerate(new RegenerateUserTurnCommand(
                "owner-a", ACTOR_USUARIO_ID, UUID.randomUUID(), "handle", "key", 5)));
        verifyNoInteractions(provider);
    }

    private static final class CapturingRegenerationPort implements CreateRegenerationPort {
        private final Optional<String> canonicalContent;
        private int calls;
        private final List<AgentOwnerId> ownerIds = new ArrayList<>();
        private final List<TurnId> turnIds = new ArrayList<>();
        private final List<String> opaqueHandles = new ArrayList<>();
        private final List<String> idempotencyKeys = new ArrayList<>();
        private final List<String> authorizationRevisions = new ArrayList<>();

        private CapturingRegenerationPort(Optional<String> canonicalContent) {
            this.canonicalContent = canonicalContent;
        }

        @Override
        public Optional<String> createRegenerationOrFindCanonical(
                AgentOwnerId ownerId,
                TurnId turnId,
                String opaqueHandle,
                String idempotencyKey,
                String authorizationRevision
        ) {
            calls++;
            ownerIds.add(ownerId);
            turnIds.add(turnId);
            opaqueHandles.add(opaqueHandle);
            idempotencyKeys.add(idempotencyKey);
            authorizationRevisions.add(authorizationRevision);
            return canonicalContent;
        }
    }

    private static final class CapturingRegeneratedCompletionPort implements CompleteRegeneratedTurnPort {
        private int calls;
        private final List<AgentOwnerId> ownerIds = new ArrayList<>();
        private final List<TurnId> turnIds = new ArrayList<>();
        private final List<String> opaqueHandles = new ArrayList<>();
        private final List<String> idempotencyKeys = new ArrayList<>();
        private final List<String> authorizationRevisions = new ArrayList<>();

        @Override
        public String completeRegeneratedTurn(
                AgentOwnerId ownerId,
                TurnId turnId,
                String opaqueHandle,
                String idempotencyKey,
                String authorizationRevision,
                String assistantContent
        ) {
            calls++;
            ownerIds.add(ownerId);
            turnIds.add(turnId);
            opaqueHandles.add(opaqueHandle);
            idempotencyKeys.add(idempotencyKey);
            authorizationRevisions.add(authorizationRevision);
            return "canonical " + assistantContent;
        }
    }

    private static final class CapturingChatCompletionPort implements ChatCompletionPort {
        private final List<String> outputs;
        private int calls;
        private final List<AgentOwnerId> ownerIds = new ArrayList<>();
        private final List<UUID> actorUsuarioIds = new ArrayList<>();
        private final List<TurnId> turnIds = new ArrayList<>();
        private final List<List<VisibleMessage>> visibleHistory = new ArrayList<>();
        private final List<List<String>> durableMemories = new ArrayList<>();
        private final List<String> prompts = new ArrayList<>();

        private CapturingChatCompletionPort(String... outputs) {
            this.outputs = List.of(outputs);
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
            ownerIds.add(ownerId);
            actorUsuarioIds.add(actorUsuarioId);
            turnIds.add(turnId);
            this.visibleHistory.add(visibleHistory);
            this.durableMemories.add(durableMemories);
            prompts.add(normalizedPrompt);
            return outputs.get(calls++);
        }
    }

    private static final class CapturingHistoryPort implements FindCompletedVisibleHistoryPort {
        private final List<VisibleMessage> history;
        private int calls;
        private final List<AgentOwnerId> ownerIds = new ArrayList<>();
        private final List<TurnId> turnIds = new ArrayList<>();
        private final List<String> opaqueHandles = new ArrayList<>();
        private final List<Integer> maximumMessages = new ArrayList<>();
        private final List<String> authorizationRevisions = new ArrayList<>();

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
            ownerIds.add(ownerId);
            turnIds.add(turnId);
            opaqueHandles.add(opaqueHandle);
            this.maximumMessages.add(maximumMessages);
            authorizationRevisions.add(authorizationRevision);
            return history;
        }
    }

    private static final class CapturingUserContentPort implements FindUserTurnContentPort {
        private final String userContent;
        private int calls;
        private final List<AgentOwnerId> ownerIds = new ArrayList<>();
        private final List<TurnId> turnIds = new ArrayList<>();
        private final List<String> opaqueHandles = new ArrayList<>();
        private final List<String> authorizationRevisions = new ArrayList<>();

        private CapturingUserContentPort(String userContent) {
            this.userContent = userContent;
        }

        @Override
        public String findUserTurnContent(AgentOwnerId ownerId, TurnId turnId, String opaqueHandle,
                                          String authorizationRevision) {
            calls++;
            ownerIds.add(ownerId);
            turnIds.add(turnId);
            opaqueHandles.add(opaqueHandle);
            authorizationRevisions.add(authorizationRevision);
            return userContent;
        }
    }

    private static final class CapturingMemoryPort implements FindEligibleDurableMemoriesPort {
        private final List<String> memories;
        private int calls;
        private final List<AgentOwnerId> ownerIds = new ArrayList<>();
        private final List<String> authorizationRevisions = new ArrayList<>();

        private CapturingMemoryPort(List<String> memories) {
            this.memories = memories;
        }

        @Override
        public List<String> findEligibleDurableMemories(AgentOwnerId ownerId, String authorizationRevision) {
            calls++;
            ownerIds.add(ownerId);
            authorizationRevisions.add(authorizationRevision);
            return memories;
        }
    }
}
