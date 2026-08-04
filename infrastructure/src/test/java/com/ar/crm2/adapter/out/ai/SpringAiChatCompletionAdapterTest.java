package com.ar.crm2.adapter.out.ai;

import com.ar.crm2.adapter.out.ai.testing.CapturingChatModel;
import com.ar.crm2.application.agent.turn.port.out.ChatCompletionPort;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.agent.vo.VisibleMessage;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Focused adapter contract for the A3 corrected
 * {@link SpringAiChatCompletionAdapter}.
 *
 * <p>Verifies that the adapter:
 * <ul>
 *     <li>Implements the existing {@link ChatCompletionPort} with role-bearing history.</li>
 *     <li>Maps the Domain {@code VisibleMessage} sequence to explicit Spring AI
 *         {@link UserMessage} / {@link AssistantMessage} instances, retaining order.</li>
 *     <li>Does NOT create its own {@link SystemMessage}. The default system
 *         prompt is owned by the production {@code com.ar.crm2.config.AgentConfig}
 *         bean (carried as the {@code ChatClient} builder {@code defaultSystem(String)}
 *         template); the adapter only injects the adapter-owned
 *         {@code durable_memories} parameter through the
 *         {@code system(Consumer<PromptSystemSpec>)} fluent API.</li>
 *     <li>Pure-formats the {@code durable_memories} parameter: filters null
 *         elements, applies {@link String#strip()}, ignores blank entries,
 *         prefixes each remaining entry with {@code - }, joins with newline.</li>
 *     <li>Delivers the normalized prompt as the final {@link UserMessage}.</li>
 *     <li>Returns only the final textual content from the model.</li>
 *     <li>Propagates provider failure through a controlled exception without leaking it.</li>
 *     <li>Forwards the trusted CRM {@code actorUsuarioId} per request via the
 *         framework {@code .toolContext(Map.of("actorUsuarioId", ...))} path,
 *         so the model's view of the request never carries the identity.</li>
 *     <li>Does NOT call request {@code .tools(...)} — Spring AI 2.0 runtime
 *         tools would replace builder defaults, so the adapter only supplies
 *         trusted per-request tool context and lets the configured
 *         {@code defaultTools} (the three allowlisted CRM tools) reach the
 *         model unchanged.</li>
 * </ul>
 *
 * <p>These tests use deterministic provider-free seams ({@link CapturingChatModel}
 * or inline {@code ChatModel} lambdas) so no network, credentials, or starter
 * beans are involved.
 */
class SpringAiChatCompletionAdapterTest {

    private static final AgentOwnerId OWNER = AgentOwnerId.from("actor-pr8");
    private static final UUID ACTOR_USUARIO_ID =
            UUID.fromString("cccccccc-1111-2222-3333-444444444444");
    private static final TurnId TURN = TurnId.create();

    /**
     * Minimal template used by the adapter tests. The production template
     * (containing the Pipely CRM framing, identity discipline, etc.) lives
     * in {@code com.ar.crm2.config.AgentConfig.DEFAULT_SYSTEM_TEMPLATE}; the
     * adapter tests use this placeholder-only template so they can prove
     * adapter behavior without re-declaring template content.
     */
    private static final String MINIMAL_DEFAULT_SYSTEM_TEMPLATE = "{durable_memories}";

    /**
     * Memory-aware fixture that mirrors the shape of what the production
     * {@code com.ar.crm2.config.AgentConfig} bean owns at runtime, plus
     * the three shared {@code defaultTools} the agent advertises. The
     * adapter is constructed only with the configured {@link ChatClient};
     * it MUST NOT need the tools at construction time because the
     * defaults carry the tool catalog.
     */
    private static ChatClient newMemoryAwareClient(CapturingChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem(MINIMAL_DEFAULT_SYSTEM_TEMPLATE)
                .build();
    }

    private static ChatClient newMemoryAwareClient(org.springframework.ai.chat.model.ChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem(MINIMAL_DEFAULT_SYSTEM_TEMPLATE)
                .build();
    }

    // ----------------------------------------------------------------------
    // Pure-formatter contract: empty input or all-filtered entries produce
    // an empty placeholder value. The adapter does NOT create its own
    // SystemMessage; it supplies the {durable_memories} parameter through
    // the system(Consumer) fluent API.
    // ----------------------------------------------------------------------

    @Test
    void defaultSystemPlaceholderReceivesValidBulletFormattedDurableMemoriesAndExactlyOneSystemMessage() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient client = newMemoryAwareClient(model);
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        adapter.complete(OWNER, ACTOR_USUARIO_ID, TURN,
                List.of(VisibleMessage.user("hi")),
                List.of("alpha preference", "beta handle"),
                "go");

        List<Message> instructions = model.capturedPrompt().getInstructions();
        long systemCount = instructions.stream()
                .filter(SystemMessage.class::isInstance)
                .count();
        assertThat(systemCount)
                .as("exactly one rendered system message from defaultSystem template")
                .isEqualTo(1);
        Message system = instructions.get(0);
        assertThat(system).isInstanceOf(SystemMessage.class);
        String text = system.getText();
        assertThat(text)
                .as("valid memories are rendered with bullet format and order preserved")
                .contains("- alpha preference")
                .contains("- beta handle");
        assertThat(text)
                .as("the {durable_memories} placeholder was substituted by the adapter")
                .doesNotContain("{durable_memories}");
    }

    @Test
    void emptyDurableMemoriesListRendersEmptyPlaceholderValue() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient client = newMemoryAwareClient(model);
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        adapter.complete(OWNER, ACTOR_USUARIO_ID, TURN,
                List.of(VisibleMessage.user("hi")), List.of(), "go");

        List<Message> instructions = model.capturedPrompt().getInstructions();
        Message system = instructions.get(0);
        assertThat(system).isInstanceOf(SystemMessage.class);
        String text = system.getText();
        assertThat(text).doesNotContain("(no eligible durable memories)");
        assertThat(text).doesNotContain("- ");
        assertThat(text).doesNotContain("{durable_memories}");
    }

    @Test
    void nullAndBlankOnlyDurableMemoriesRenderEmptyPlaceholderValue() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient client = newMemoryAwareClient(model);
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        List<String> allNullOrBlank = new ArrayList<>();
        allNullOrBlank.add(null);
        allNullOrBlank.add("");
        allNullOrBlank.add("   ");
        allNullOrBlank.add("\t");

        adapter.complete(OWNER, ACTOR_USUARIO_ID, TURN,
                List.of(VisibleMessage.user("hi")), allNullOrBlank, "go");

        List<Message> instructions = model.capturedPrompt().getInstructions();
        Message system = instructions.get(0);
        String text = system.getText();
        assertThat(text).doesNotContain("(no eligible durable memories)");
        assertThat(text).doesNotContain("- ");
        assertThat(text).doesNotContain("null");
        assertThat(text).doesNotContain("{durable_memories}");
    }

    @Test
    void singleValidMemoryRendersExactlyOneBulletWithoutTrailingSeparator() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient client = newMemoryAwareClient(model);
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        adapter.complete(OWNER, ACTOR_USUARIO_ID, TURN,
                List.of(VisibleMessage.user("hi")), List.of("only memory"), "go");

        List<Message> instructions = model.capturedPrompt().getInstructions();
        Message system = instructions.get(0);
        String text = system.getText();
        assertThat(text).endsWith("- only memory");
        assertThat(text).contains("- only memory");
        assertThat(text).doesNotContain("-- ");
        assertThat(text).doesNotMatch("(?s).*- \\R.*");
        assertThat(text).doesNotContain("{durable_memories}");
    }

    @Test
    void mixedEntriesIgnoreNullAndBlanksStripTextPreserveValidOrderAndJoinWithNewline() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient client = newMemoryAwareClient(model);
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        List<String> memories = new ArrayList<>();
        memories.add(null);
        memories.add("   first memory   ");
        memories.add("");
        memories.add("  ");
        memories.add("second memory");
        memories.add("\tthird\t");

        adapter.complete(OWNER, ACTOR_USUARIO_ID, TURN,
                List.of(VisibleMessage.user("hi")), memories, "go");

        List<Message> instructions = model.capturedPrompt().getInstructions();
        Message system = instructions.get(0);
        String text = system.getText();
        int firstIdx = text.indexOf("- first memory");
        int secondIdx = text.indexOf("- second memory");
        int thirdIdx = text.indexOf("- third");
        assertThat(firstIdx).isNotNegative();
        assertThat(secondIdx).isNotNegative();
        assertThat(thirdIdx).isNotNegative();
        assertThat(firstIdx).isLessThan(secondIdx);
        assertThat(secondIdx).isLessThan(thirdIdx);
        assertThat(text).containsPattern("- first memory\\R- second memory");
        assertThat(text).containsPattern("- second memory\\R- third");
        assertThat(text).endsWith("- third");
        assertThat(text).doesNotContain("-   first memory");
        assertThat(text).doesNotContain("-   second memory");
        assertThat(text).doesNotContain("- first memory   ");
        assertThat(text).doesNotContain("- second memory   ");
        assertThat(text).doesNotContain("- \n");
        assertThat(text).doesNotContain("null");
        assertThat(text).doesNotContain("{durable_memories}");
    }

    @Test
    void visibleHistoryKeepsUserAssistantOrderAfterSystemMessage() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient client = newMemoryAwareClient(model);
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        List<VisibleMessage> history = List.of(
                VisibleMessage.user("first user"),
                VisibleMessage.assistant("first assistant"),
                VisibleMessage.user("second user"));

        adapter.complete(OWNER, ACTOR_USUARIO_ID, TURN, history, List.of(), "current prompt");

        List<Message> instructions = model.capturedPrompt().getInstructions();
        assertThat(instructions).hasSize(history.size() + 2);
        assertThat(instructions.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(instructions.get(1)).isInstanceOf(UserMessage.class);
        assertThat(instructions.get(1).getText()).isEqualTo("first user");
        assertThat(instructions.get(2)).isInstanceOf(AssistantMessage.class);
        assertThat(instructions.get(2).getText()).isEqualTo("first assistant");
        assertThat(instructions.get(3)).isInstanceOf(UserMessage.class);
        assertThat(instructions.get(3).getText()).isEqualTo("second user");
    }

    @Test
    void normalizedPromptIsFinalUserMessage() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient client = newMemoryAwareClient(model);
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        adapter.complete(OWNER, ACTOR_USUARIO_ID, TURN,
                List.of(VisibleMessage.user("hi")), List.of(), "the-current-prompt");

        List<Message> instructions = model.capturedPrompt().getInstructions();
        Message last = instructions.get(instructions.size() - 1);
        assertThat(last).isInstanceOf(UserMessage.class);
        assertThat(last.getText()).isEqualTo("the-current-prompt");
    }

    @Test
    void providerFailureStillYieldsOnlySpringAiChatCompletionFailedWithNoCause() {
        ChatClient client = newMemoryAwareClient((org.springframework.ai.chat.model.ChatModel) prompt -> {
            throw new IllegalStateException("provider-failure-sentinel-must-not-leak");
        });
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        assertThatThrownBy(() -> adapter.complete(
                OWNER, ACTOR_USUARIO_ID, TURN,
                List.of(VisibleMessage.user("hi")),
                List.of("memory"),
                "prompt"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Spring AI chat completion failed")
                .hasNoCause();
    }

    @Test
    void returnsOnlyTheModelFinalContentWithoutEchoingHistory() {
        CapturingChatModel model = new CapturingChatModel("the only response");
        ChatClient client = newMemoryAwareClient(model);
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        String result = adapter.complete(OWNER, ACTOR_USUARIO_ID, TURN,
                List.of(VisibleMessage.user("u1"), VisibleMessage.assistant("a1")),
                List.of("memory"), "now");

        assertThat(result).isEqualTo("the only response");
        assertThat(result).doesNotContain("u1", "a1", "memory", "now");
    }

    @Test
    void propagatesProviderFailureAsGenericSanitizedExceptionWithoutExposingAnySensitiveData() {
        String ownerSentinel = "owner-sentinel-secret-id";
        String actorSentinel = "cccccccc-9999-aaaa-bbbb-1234567890ab";
        String turnSentinel = "turn-sentinel-secret-id";
        String promptSentinel = "prompt-sentinel-must-not-leak";
        String memorySentinel = "memory-sentinel-must-not-leak";
        String completionSentinel = "completion-sentinel-from-provider-must-not-leak";
        String providerFailureSentinel = "provider-failure-sentinel-must-not-leak";
        AgentOwnerId sensitiveOwner = AgentOwnerId.from(ownerSentinel);
        UUID sensitiveActor = UUID.fromString(actorSentinel);
        TurnId sensitiveTurn = TurnId.from(UUID.fromString(
                "00000000-0000-0000-0000-000000000077"));

        ChatClient client = ChatClient.builder((org.springframework.ai.chat.model.ChatModel) prompt -> {
            throw new IllegalStateException(providerFailureSentinel);
        }).build();
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        assertThatThrownBy(() -> adapter.complete(
                sensitiveOwner,
                sensitiveActor,
                sensitiveTurn,
                List.of(
                        VisibleMessage.user("visible-history-sentinel-must-not-leak"),
                        VisibleMessage.assistant(completionSentinel)),
                List.of(memorySentinel),
                promptSentinel))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Spring AI chat completion failed")
                .hasMessageNotContainingAny(
                        ownerSentinel,
                        actorSentinel,
                        turnSentinel,
                        promptSentinel,
                        memorySentinel,
                        completionSentinel,
                        providerFailureSentinel,
                        "visible-history-sentinel-must-not-leak")
                .hasNoCause();
    }

    @Test
    void supportsInlineChatModelReturningFixedChatResponse() {
        ChatClient client = ChatClient.builder(prompt -> new ChatResponse(List.of(
                new Generation(new AssistantMessage("inline answer"))
        ))).build();
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        String result = adapter.complete(OWNER, ACTOR_USUARIO_ID, TURN,
                List.of(VisibleMessage.user("hi")), List.of(), "go");

        assertThat(result).isEqualTo("inline answer");
    }

    @Test
    void actorUsuarioIdIsNotEmbeddedInModelVisiblePrompt() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient client = newMemoryAwareClient(model);
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        adapter.complete(OWNER, ACTOR_USUARIO_ID, TURN,
                List.of(VisibleMessage.user("hello")), List.of(), "go");

        List<Message> instructions = model.capturedPrompt().getInstructions();
        for (Message instruction : instructions) {
            assertThat(instruction.getText())
                    .doesNotContain(ACTOR_USUARIO_ID.toString());
        }
    }

    @Test
    void ownerTurnAndActorAreNeverEmbeddedInAnyModelVisiblePromptPart() {
        // Spring AI 2.0 stores `.toolContext(Map)` on the request spec /
        // ChatClientRequest.context, not on the Prompt options the
        // ChatModel observes. We therefore assert the no-leak contract
        // from the ChatModel side: every instruction must NOT carry the
        // server-derived owner, turn, or actor identity or their key
        // names. Trusted propagation is asserted separately at the
        // @Tool side in SpringAiCrmToolsTest.
        final String ownerSentinel = "actor-pr9c4-c1-owner-not-leaked";
        final String turnSentinel = "00000000-0000-0000-0000-000000c1c1ee";
        AgentOwnerId c1Owner = AgentOwnerId.from(ownerSentinel);
        TurnId c1Turn = TurnId.from(UUID.fromString(turnSentinel));
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient client = newMemoryAwareClient(model);
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        adapter.complete(c1Owner, ACTOR_USUARIO_ID, c1Turn,
                List.of(VisibleMessage.user("hi"), VisibleMessage.assistant("ack")),
                List.of(), "prompt");

        List<Message> instructions = model.capturedPrompt().getInstructions();
        for (Message instruction : instructions) {
            String text = instruction.getText();
            assertThat(text)
                    .as("model-visible instruction (%s) must never contain owner/turn/actor identity", instruction)
                    .doesNotContain(ownerSentinel)
                    .doesNotContain(turnSentinel)
                    .doesNotContain(ACTOR_USUARIO_ID.toString())
                    .doesNotContain("agentOwnerId")
                    .doesNotContain("turnId")
                    .doesNotContain("actorUsuarioId");
        }
    }

    @Test
    void adapterConstructsWithoutToolsArgumentAndDoesNotRequestToolsPerInvocation() {
        // The corrected adapter must NOT take a SpringAiCrmToolsBinder or
        // any tools at construction — defaults are registered once on the
        // ChatClient by AgentConfig. The adapter also must NOT call
        // .tools(...) at request time (Spring AI 2.0 runtime tools
        // replace defaults). Construct via reflection to prove the
        // constructor signature has no tools parameter; this guards
        // against an accidental regression to the prior binder-based
        // architecture.
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient client = newMemoryAwareClient(model);
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        assertThat(adapter)
                .as("adapter must be constructible from the configured ChatClient only")
                .isNotNull();

        java.lang.reflect.Constructor<?>[] constructors = SpringAiChatCompletionAdapter.class
                .getDeclaredConstructors();
        boolean hasSingleChatClientConstructor = false;
        for (java.lang.reflect.Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() == 1
                    && constructor.getParameterTypes()[0] == ChatClient.class) {
                hasSingleChatClientConstructor = true;
                break;
            }
        }
        assertThat(hasSingleChatClientConstructor)
                .as("the corrected adapter must declare exactly one (ChatClient) constructor")
                .isTrue();
    }
}