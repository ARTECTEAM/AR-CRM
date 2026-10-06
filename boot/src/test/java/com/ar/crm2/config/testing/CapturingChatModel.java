package com.ar.crm2.config.testing;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/**
 * Deterministic provider-free {@link ChatModel} seam for boot-layer
 * tests that need to inspect the rendered prompt produced by the
 * configured Spring AI 2.0 {@link org.springframework.ai.chat.client.ChatClient}.
 *
 * <p>This is a local copy of the seam that lives under
 * {@code com.ar.crm2.adapter.out.ai.testing.CapturingChatModel} in the
 * {@code infrastructure} module. The two modules cannot share a test
 * source folder, so each keeps its own minimal deterministic fixture.
 * The two are structurally identical \u2014 intentionally so the same
 * end-to-end rendering invariant can be proved at every layer.
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
