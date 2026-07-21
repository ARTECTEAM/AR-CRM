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
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider-neutral Spring AI 2.0 adapter for the Application
 * {@link ChatCompletionPort}.
 *
 * <p>PR8 owns the adapter shape only:
 * <ul>
 *     <li>Maps ordered Domain {@link VisibleMessage} entries to explicit
 *         {@link UserMessage}/{@link AssistantMessage} instances while preserving order.</li>
 *     <li>Keeps durable owner memory as a separate, labelled
 *         {@link SystemMessage} — never folded into the user turn.</li>
 *     <li>Builds the request with an explicit {@code Prompt(List<Message>)} and
 *         returns only the final textual content.</li>
 * </ul>
 *
 * <p>Tools, advisors, ChatMemory, RAG, MCP, streaming, structured output,
 * credential wiring, and provider-specific starter configuration are intentionally
 * absent and belong to later PRs (PR9–PR13).
 */
@RequiredArgsConstructor
public class SpringAiChatCompletionAdapter implements ChatCompletionPort {

    private static final String MEMORY_SECTION_HEADER =
            "DURABLE OWNER MEMORY (separate reference; do not echo or address the owner directly):";

    private final ChatClient chatClient;

    @Override
    public String complete(
            AgentOwnerId ownerId,
            TurnId turnId,
            List<VisibleMessage> visibleHistory,
            List<String> durableMemories,
            String normalizedPrompt
    ) {
        List<Message> messages = new ArrayList<>(visibleHistory.size() + 2);
        messages.add(buildMemorySystemMessage(durableMemories));
        for (VisibleMessage visible : visibleHistory) {
            messages.add(toSpringAiMessage(visible));
        }
        messages.add(new UserMessage(normalizedPrompt));
        Prompt prompt = new Prompt(messages);
        try {
            return chatClient.prompt(prompt).call().content();
        } catch (RuntimeException ex) {
            // Controlled failure boundary: do NOT embed owner/turn identifiers,
            // prompt content, durable memory content, completion content, or
            // any provider-supplied message/sentinel here. Do NOT retain the
            // raw provider exception as a cause — the provider exception can
            // expose stack/message details that must not surface through this
            // adapter's public failure path. The cause is intentionally dropped.
            throw new IllegalStateException("Spring AI chat completion failed");
        }
    }

    private static Message buildMemorySystemMessage(List<String> durableMemories) {
        StringBuilder builder = new StringBuilder(MEMORY_SECTION_HEADER);
        if (durableMemories == null || durableMemories.isEmpty()) {
            builder.append("\n(no eligible durable memories)");
        } else {
            for (String memory : durableMemories) {
                builder.append('\n').append("- ").append(memory);
            }
        }
        return new SystemMessage(builder.toString());
    }

    private static Message toSpringAiMessage(VisibleMessage visible) {
        if (visible.role() == VisibleMessageRole.ASSISTANT) {
            return new AssistantMessage(visible.content());
        }
        return new UserMessage(visible.content());
    }
}
