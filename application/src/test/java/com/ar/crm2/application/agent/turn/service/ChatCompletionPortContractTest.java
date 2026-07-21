package com.ar.crm2.application.agent.turn.service;

import com.ar.crm2.application.agent.turn.port.out.ChatCompletionPort;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.agent.vo.VisibleMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatCompletionPortContractTest {

    @Test
    void completesWithExplicitDomainAndStandardValues() {
        ChatCompletionPort port = (ownerId, turnId, history, memories, prompt) ->
                ownerId.value() + ":" + turnId.value() + ":" + history.size() + ":" + memories.size() + ":" + prompt;
        TurnId turnId = TurnId.from(java.util.UUID.fromString("7a9e72de-0df7-4899-8763-1bdecd8422e3"));
        VisibleMessage userHistory = VisibleMessage.user("previous user message");

        String completion = port.complete(AgentOwnerId.from("actor-a"), turnId,
                List.of(userHistory), List.of("remember preference"), "current prompt");

        assertEquals("actor-a:7a9e72de-0df7-4899-8763-1bdecd8422e3:1:1:current prompt", completion);
    }

    @Test
    void acceptsEmptyContextWithoutSyntheticRequestOrResponseTypes() {
        ChatCompletionPort port = (ownerId, turnId, history, memories, prompt) -> prompt.toUpperCase();

        String completion = port.complete(AgentOwnerId.from("actor-b"), TurnId.create(), List.of(), List.of(), "hello");

        assertEquals("HELLO", completion);
    }
}
