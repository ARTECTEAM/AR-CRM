package com.ar.crm2.adapter.out.ai;

import com.ar.crm2.application.agent.turn.port.out.ChatCompletionPort;
import com.ar.crm2.model.agent.enums.VisibleMessageRole;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.agent.vo.VisibleMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Provider-neutral Spring AI 2.0 adapter for the Application
 * {@link ChatCompletionPort}.
 *
 * <p>The adapter owns the following responsibilities and nothing else:
 * <ul>
 *     <li>Maps ordered Domain {@link VisibleMessage} entries to explicit
 *         {@link UserMessage} / {@link AssistantMessage} instances while
 *         preserving order.</li>
 *     <li>Supplies the {@code durable_memories} placeholder parameter for
 *         the ChatClient builder's {@code defaultSystem(String)} template
 *         via the {@code system(Consumer<PromptSystemSpec>)} fluent API.
 *         The adapter does NOT create its own
 *         {@link org.springframework.ai.chat.messages.SystemMessage} — that
 *         responsibility belongs to the production
 *         {@code com.ar.crm2.config.AgentConfig} bean, which owns the
 *         ChatClient builder and the template string at boot-composition
 *         time.</li>
 *     <li>Bullet-formats durable memories (filters null elements, applies
 *         {@link String#strip()}, ignores blank entries, prefixes each
 *         remaining entry with {@code - }, joins with newline).</li>
 *     <li>Forwards the trusted CRM {@code actorUsuarioId} per request via
 *         the framework {@code .toolContext(Map.of("actorUsuarioId", ...))}
 *         call. The configured {@link ChatClient} already carries the three
 *         allowlisted CRM tools through {@code defaultTools(tools)}; the
 *         adapter does NOT call request {@code .tools(...)} because Spring
 *         AI 2.0 runtime tools replace builder defaults. Identity stays
 *         outside the model-visible schema.</li>
 *     <li>Returns only the final textual content from the model, and
 *         propagates provider failure through a controlled
 *         {@link IllegalStateException} without leaking the cause.</li>
 * </ul>
 *
 * <p>Provider-specific starter configuration, ChatMemory, RAG, MCP,
 * streaming, structured output, and credential wiring are intentionally
 * absent and belong to later PRs (PR9–PR13).
 */
@RequiredArgsConstructor
public class SpringAiChatCompletionAdapter implements ChatCompletionPort {

    static final String ACTOR_CONTEXT_KEY = "actorUsuarioId";

    private final ChatClient chatClient;

    @Override
    public String complete(
            AgentOwnerId ownerId,
            UUID actorUsuarioId,
            TurnId turnId,
            List<VisibleMessage> visibleHistory,
            List<String> durableMemories,
            String normalizedPrompt
    ) {
        List<Message> historyMessages = visibleHistory.stream()
                .map(SpringAiChatCompletionAdapter::toSpringAiMessage)
                .toList();
        try {
            return chatClient.prompt()
                    .system(system -> system.param(
                            "durable_memories",
                            formatDurableMemories(durableMemories)
                    ))
                    .messages(historyMessages)
                    .user(normalizedPrompt)
                    .toolContext(Map.of(ACTOR_CONTEXT_KEY, actorUsuarioId))
                    .call()
                    .content();
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Spring AI chat completion failed");
        }
    }

    private static String formatDurableMemories(List<String> durableMemories) {
        return durableMemories.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(memory -> !memory.isBlank())
                .map(memory -> "- " + memory)
                .collect(Collectors.joining("\n"));
    }

    private static Message toSpringAiMessage(VisibleMessage visible) {
        if (visible.role() == VisibleMessageRole.ASSISTANT) {
            return new AssistantMessage(visible.content());
        }
        return new UserMessage(visible.content());
    }
}