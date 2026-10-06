package com.ar.crm2.application.agent.turn.service;

import com.ar.crm2.application.agent.turn.port.out.ChatCompletionPort;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.agent.vo.VisibleMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatCompletionPortContractTest {

    @Test
    void completesWithExplicitDomainAndStandardValues() {
        UUID actorUsuarioId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        ChatCompletionPort port = (ownerId, actorUsuarioIdValue, turnId, history, memories, prompt) ->
                ownerId.value() + ":" + actorUsuarioIdValue + ":"
                        + turnId.value() + ":" + history.size() + ":" + memories.size() + ":" + prompt;
        TurnId turnId = TurnId.from(UUID.fromString("7a9e72de-0df7-4899-8763-1bdecd8422e3"));
        VisibleMessage userHistory = VisibleMessage.user("previous user message");

        String completion = port.complete(AgentOwnerId.from("actor-a"), actorUsuarioId, turnId,
                List.of(userHistory), List.of("remember preference"), "current prompt");

        assertEquals(
                "actor-a:aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee:7a9e72de-0df7-4899-8763-1bdecd8422e3:1:1:current prompt",
                completion);
    }

    @Test
    void acceptsEmptyContextWithoutSyntheticRequestOrResponseTypes() {
        UUID actorUsuarioId = UUID.fromString("ffffffff-eeee-dddd-cccc-bbbbbbbbbbbb");
        ChatCompletionPort port = (ownerId, actorUsuarioIdValue, turnId, history, memories, prompt) ->
                actorUsuarioIdValue + ":" + prompt.toUpperCase();

        String completion = port.complete(AgentOwnerId.from("actor-b"), actorUsuarioId, TurnId.create(),
                List.of(), List.of(), "hello");

        assertEquals("ffffffff-eeee-dddd-cccc-bbbbbbbbbbbb:HELLO", completion);
    }

    @Test
    void forwardsActorUsuarioIdIndependentOfOwnerSubjectValue() {
        UUID actorUsuarioId = UUID.fromString("12345678-1234-1234-1234-123456789012");
        ChatCompletionPort port = (ownerId, actorUsuarioIdValue, turnId, history, memories, prompt) ->
                "ownerSubject=" + ownerId.value() + ";actorUsuarioId=" + actorUsuarioIdValue;

        String completion = port.complete(AgentOwnerId.from("owner-subject-not-crm-id"), actorUsuarioId,
                TurnId.create(), List.of(), List.of(), "p");

        assertEquals("ownerSubject=owner-subject-not-crm-id;actorUsuarioId=12345678-1234-1234-1234-123456789012",
                completion);
    }
}
