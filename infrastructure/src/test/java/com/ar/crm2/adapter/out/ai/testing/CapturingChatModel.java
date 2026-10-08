package com.ar.crm2.adapter.out.ai.testing;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/**
 * Deterministic provider-free {@link ChatModel} seam for adapter tests.
 *
 * <p>Captures the last {@link Prompt} sent through {@link ChatModel#call(Prompt)}
 * and returns a fixed {@link ChatResponse}. No network, credentials, or starter
 * beans are involved.
 */
public final class CapturingChatModel implements ChatModel {

    private final String fixedAnswer;
    private Prompt capturedPrompt;

    public CapturingChatModel(String fixedAnswer) {
        this.fixedAnswer = fixedAnswer;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        this.capturedPrompt = prompt;
        return new ChatResponse(List.of(new Generation(new AssistantMessage(fixedAnswer))));
    }

    public Prompt capturedPrompt() {
        if (capturedPrompt == null) {
            throw new IllegalStateException("The capturing model has not received a prompt yet.");
        }
        return capturedPrompt;
    }
}
