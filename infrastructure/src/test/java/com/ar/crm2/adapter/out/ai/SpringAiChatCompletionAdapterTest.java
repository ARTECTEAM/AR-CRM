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
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Focused adapter contract for PR8 Spring AI base.
 *
 * <p>Verifies that {@link SpringAiChatCompletionAdapter}:
 * <ul>
 *     <li>Implements the existing {@link ChatCompletionPort} with role-bearing history.</li>
 *     <li>Maps the Domain {@code VisibleMessage} sequence to explicit Spring AI
 *         {@link UserMessage} / {@link AssistantMessage} instances, retaining order.</li>
 *     <li>Keeps durable owner memory as a separate labelled {@link SystemMessage}.</li>
 *     <li>Delivers the normalized prompt as the final {@link UserMessage}.</li>
 *     <li>Returns only the final textual content from the model.</li>
 *     <li>Propagates provider failure through a controlled exception without leaking it.</li>
 * </ul>
 *
 * <p>The tests use deterministic provider-free seams ({@link CapturingChatModel} or
 * inline {@code ChatModel} lambdas) so no network, credentials, or starter beans
 * are involved.
 */
class SpringAiChatCompletionAdapterTest {

    private static final AgentOwnerId OWNER = AgentOwnerId.from("actor-pr8");
    private static final TurnId TURN = TurnId.create();

    @Test
    void mapsOrderedVisibleHistoryToExplicitUserAndAssistantMessagesAndReturnsModelContent() {
        CapturingChatModel model = new CapturingChatModel("assistant reply");
        ChatClient client = ChatClient.builder(model).build();
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);
        List<VisibleMessage> history = List.of(
                VisibleMessage.user("first user"),
                VisibleMessage.assistant("first assistant"),
                VisibleMessage.user("second user")
        );

        String result = adapter.complete(OWNER, TURN, history, List.of(), "current prompt");

        assertThat(result).isEqualTo("assistant reply");
        List<Message> instructions = model.capturedPrompt().getInstructions();
        assertThat(instructions).hasSize(history.size() + 2);

        assertThat(instructions.get(0)).isInstanceOf(SystemMessage.class);

        assertThat(instructions.get(1)).isInstanceOf(UserMessage.class);
        assertThat(instructions.get(1).getText()).isEqualTo("first user");

        assertThat(instructions.get(2)).isInstanceOf(AssistantMessage.class);
        assertThat(instructions.get(2).getText()).isEqualTo("first assistant");

        assertThat(instructions.get(3)).isInstanceOf(UserMessage.class);
        assertThat(instructions.get(3).getText()).isEqualTo("second user");

        assertThat(instructions.get(4)).isInstanceOf(UserMessage.class);
        assertThat(instructions.get(4).getText()).isEqualTo("current prompt");
    }

    @Test
    void keepsDurableOwnerMemorySeparateAsLabelledSystemMessage() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient client = ChatClient.builder(model).build();
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);
        List<String> memories = List.of("preference: invoicing on Fridays", "owner handle: acme");

        adapter.complete(OWNER, TURN, List.of(VisibleMessage.user("hi")), memories, "go");

        List<Message> instructions = model.capturedPrompt().getInstructions();
        assertThat(instructions).hasSize(3);
        Message memory = instructions.get(0);
        assertThat(memory).isInstanceOf(SystemMessage.class);
        String memoryText = memory.getText();
        assertThat(memoryText).contains("DURABLE OWNER MEMORY");
        assertThat(memoryText).contains("preference: invoicing on Fridays");
        assertThat(memoryText).contains("owner handle: acme");

        assertThat(instructions.get(1)).isInstanceOf(UserMessage.class);
        assertThat(instructions.get(1).getText()).isEqualTo("hi");
        assertThat(instructions.get(2)).isInstanceOf(UserMessage.class);
        assertThat(instructions.get(2).getText()).isEqualTo("go");
    }

    @Test
    void rendersEmptyMemoryAsExplicitlyLabelledSystemMessage() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient client = ChatClient.builder(model).build();
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        adapter.complete(OWNER, TURN, List.of(), List.of(), "solo prompt");

        List<Message> instructions = model.capturedPrompt().getInstructions();
        assertThat(instructions).hasSize(2);
        assertThat(instructions.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(instructions.get(0).getText()).contains("DURABLE OWNER MEMORY");
        assertThat(instructions.get(1)).isInstanceOf(UserMessage.class);
        assertThat(instructions.get(1).getText()).isEqualTo("solo prompt");
    }

    @Test
    void returnsOnlyTheModelFinalContentWithoutEchoingHistory() {
        CapturingChatModel model = new CapturingChatModel("the only response");
        ChatClient client = ChatClient.builder(model).build();
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        String result = adapter.complete(OWNER, TURN,
                List.of(VisibleMessage.user("u1"), VisibleMessage.assistant("a1")),
                List.of("memory"), "now");

        assertThat(result).isEqualTo("the only response");
        assertThat(result).doesNotContain("u1", "a1", "memory", "now");
    }

    @Test
    void propagatesProviderFailureAsGenericSanitizedExceptionWithoutExposingAnySensitiveData() {
        // Sentinels injected into every sensitive surface the verifier expects
        // the adapter to NOT leak through the produced exception.
        String ownerSentinel = "owner-sentinel-secret-id";
        String turnSentinel = "turn-sentinel-secret-id";
        String promptSentinel = "prompt-sentinel-must-not-leak";
        String memorySentinel = "memory-sentinel-must-not-leak";
        String completionSentinel = "completion-sentinel-from-provider-must-not-leak";
        String providerFailureSentinel = "provider-failure-sentinel-must-not-leak";
        AgentOwnerId sensitiveOwner = AgentOwnerId.from(ownerSentinel);
        TurnId sensitiveTurn = TurnId.from(java.util.UUID.fromString(
                "00000000-0000-0000-0000-000000000077"));

        ChatClient client = ChatClient.builder((org.springframework.ai.chat.model.ChatModel) prompt -> {
            throw new IllegalStateException(providerFailureSentinel);
        }).build();
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        assertThatThrownBy(() -> adapter.complete(
                sensitiveOwner,
                sensitiveTurn,
                List.of(
                        VisibleMessage.user("visible-history-sentinel-must-not-leak"),
                        VisibleMessage.assistant(completionSentinel)),
                List.of(memorySentinel),
                promptSentinel))
                .isInstanceOf(IllegalStateException.class)
                // Stable, generic message — fixed contract, not parameterized by sensitive data.
                .hasMessage("Spring AI chat completion failed")
                .hasMessageNotContainingAny(
                        ownerSentinel,
                        turnSentinel,
                        promptSentinel,
                        memorySentinel,
                        completionSentinel,
                        providerFailureSentinel,
                        "visible-history-sentinel-must-not-leak")
                // No cause retained — the raw provider exception must not expose
                // provider message/stack data through the produced exception.
                .hasNoCause()
                .hasCause(null);
    }

    @Test
    void preservesUserRoleMappingAcrossMultipleConsecutiveUserMessages() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient client = ChatClient.builder(model).build();
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);
        List<VisibleMessage> history = List.of(
                VisibleMessage.user("a"),
                VisibleMessage.user("b"),
                VisibleMessage.user("c")
        );

        adapter.complete(OWNER, TURN, history, List.of(), "z");

        List<Message> instructions = model.capturedPrompt().getInstructions();
        assertThat(instructions).hasSize(5);
        assertThat(instructions.get(0)).isInstanceOf(SystemMessage.class);
        for (int index = 0; index < 3; index++) {
            assertThat(instructions.get(index + 1)).isInstanceOf(UserMessage.class);
            assertThat(instructions.get(index + 1).getText()).isEqualTo(history.get(index).content());
        }
        assertThat(instructions.get(4)).isInstanceOf(UserMessage.class);
        assertThat(instructions.get(4).getText()).isEqualTo("z");
    }

    @Test
    void buildsPromptWithExplicitMessageList() {
        CapturingChatModel model = new CapturingChatModel("pong");
        ChatClient client = ChatClient.builder(model).build();
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        String result = adapter.complete(OWNER, TURN,
                List.of(VisibleMessage.user("ping")), List.of(), "current");

        assertThat(result).isEqualTo("pong");
        Prompt captured = model.capturedPrompt();
        assertThat(captured).isNotNull();
        assertThat(captured.getInstructions()).hasSize(3);
        assertThat(captured.getInstructions().get(0)).isInstanceOf(SystemMessage.class);
        assertThat(captured.getInstructions().get(1).getText()).isEqualTo("ping");
        assertThat(captured.getInstructions().get(2).getText()).isEqualTo("current");
    }

    @Test
    void preservesAssistantSpeakerProvenanceInAlternatingConversation() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient client = ChatClient.builder(model).build();
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);
        List<VisibleMessage> history = List.of(
                VisibleMessage.assistant("earlier assistant"),
                VisibleMessage.user("follow-up question")
        );

        adapter.complete(OWNER, TURN, history, List.of(), "next");

        List<Message> instructions = model.capturedPrompt().getInstructions();
        assertThat(instructions).hasSize(4);
        assertThat(instructions.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(instructions.get(1)).isInstanceOf(AssistantMessage.class);
        assertThat(instructions.get(1).getText()).isEqualTo("earlier assistant");
        assertThat(instructions.get(2)).isInstanceOf(UserMessage.class);
        assertThat(instructions.get(2).getText()).isEqualTo("follow-up question");
        assertThat(instructions.get(3)).isInstanceOf(UserMessage.class);
        assertThat(instructions.get(3).getText()).isEqualTo("next");
    }

    @Test
    void supportsInlineChatModelReturningFixedChatResponse() {
        ChatClient client = ChatClient.builder(prompt -> new ChatResponse(List.of(
                new Generation(new AssistantMessage("inline answer"))
        ))).build();
        SpringAiChatCompletionAdapter adapter = new SpringAiChatCompletionAdapter(client);

        String result = adapter.complete(OWNER, TURN,
                List.of(VisibleMessage.user("hi")), List.of(), "go");

        assertThat(result).isEqualTo("inline answer");
    }
}
