package com.ar.crm2.model.agent.entity;

import com.ar.crm2.exception.AgentTurnStateTransitionException;
import com.ar.crm2.exception.InvariantViolationException;
import com.ar.crm2.model.agent.enums.TurnState;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.ConversationId;
import com.ar.crm2.model.agent.vo.TurnId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentConversationTest {

    @Nested
    class Identifiers {

        @Test
        void ownerId_trimsValidSubject() {
            AgentOwnerId ownerId = AgentOwnerId.from("  user-42  ");

            assertThat(ownerId.value()).isEqualTo("user-42");
        }

        @Test
        void ownerId_rejectsBlankSubject() {
            assertThatThrownBy(() -> AgentOwnerId.from("  "))
                    .isInstanceOf(InvariantViolationException.class);
        }

        @Test
        void ownerId_rejectsNullSubject() {
            assertThatThrownBy(() -> AgentOwnerId.from(null))
                    .isInstanceOf(InvariantViolationException.class);
        }

        @Test
        void identifiers_reconstructValidatedValues() {
            UUID value = UUID.randomUUID();

            assertThat(AgentOwnerId.from("user-42").value()).isEqualTo("user-42");
            assertThat(TurnId.from(value).value()).isEqualTo(value);
            assertThat(ConversationId.from(value).value()).isEqualTo(value);
        }

        @Test
        void turnId_rejectsNullUuid() {
            assertThatThrownBy(() -> TurnId.from(null))
                    .isInstanceOf(InvariantViolationException.class);
        }

        @Test
        void conversationId_rejectsNullUuid() {
            assertThatThrownBy(() -> ConversationId.from(null))
                    .isInstanceOf(InvariantViolationException.class);
        }
    }

    @Nested
    class ConversationLifecycle {

        @Test
        void createsOneOwnerBoundConversationAndPreparedTurn() {
            AgentOwnerId ownerId = AgentOwnerId.from("user-42");
            Conversation conversation = Conversation.create(ownerId);
            TurnId turnId = TurnId.from(UUID.randomUUID());

            AgentTurn turn = conversation.prepareTurn(turnId);

            assertThat(conversation.getOwnerId()).isEqualTo(ownerId);
            assertThat(turn.getId()).isEqualTo(turnId);
            assertThat(turn.getConversationId()).isEqualTo(conversation.getId());
            assertThat(turn.getState()).isEqualTo(TurnState.PREPARED);
        }

        @Test
        void completesPreparedTurn() {
            AgentTurn preparedTurn = preparedTurn();

            AgentTurn completedTurn = preparedTurn.complete();

            assertThat(completedTurn.getId()).isEqualTo(preparedTurn.getId());
            assertThat(completedTurn.getConversationId()).isEqualTo(preparedTurn.getConversationId());
            assertThat(completedTurn.getState()).isEqualTo(TurnState.COMPLETED);
        }

        @Test
        void completingTurn_keepsOriginalPreparedTurnUnchanged() {
            AgentTurn preparedTurn = preparedTurn();

            preparedTurn.complete();

            assertThat(preparedTurn.getState()).isEqualTo(TurnState.PREPARED);
        }

        @Test
        void rejectsRepeatedCompletion() {
            AgentTurn completedTurn = preparedTurn().complete();

            assertThatThrownBy(completedTurn::complete)
                    .isInstanceOf(AgentTurnStateTransitionException.class);
        }

        @Test
        void rejectsPreparingTurnWithoutIdentity() {
            Conversation conversation = Conversation.create(AgentOwnerId.from("user-42"));

            assertThatThrownBy(() -> conversation.prepareTurn(null))
                    .isInstanceOf(InvariantViolationException.class);
        }

        @Test
        void entitiesAreNonFinalIdentityBasedDomainEntities() {
            ConversationId conversationId = ConversationId.from(UUID.randomUUID());
            AgentOwnerId ownerId = AgentOwnerId.from("user-42");
            Conversation firstConversation = Conversation.reconstitute(conversationId, ownerId);
            Conversation sameConversation = Conversation.reconstitute(conversationId, ownerId);
            Conversation otherConversation = Conversation.create(ownerId);
            TurnId turnId = TurnId.from(UUID.randomUUID());
            AgentTurn firstTurn = AgentTurn.reconstitute(turnId, conversationId, TurnState.PREPARED);
            AgentTurn sameTurn = AgentTurn.reconstitute(turnId, conversationId, TurnState.COMPLETED);
            AgentTurn otherTurn = Conversation.create(ownerId).prepareTurn(TurnId.from(UUID.randomUUID()));

            assertThat(Conversation.class.isRecord()).isFalse();
            assertThat(AgentTurn.class.isRecord()).isFalse();
            assertThat(Modifier.isFinal(Conversation.class.getModifiers())).isFalse();
            assertThat(Modifier.isFinal(AgentTurn.class.getModifiers())).isFalse();
            assertThat(firstConversation).isEqualTo(sameConversation).hasSameHashCodeAs(sameConversation);
            assertThat(firstConversation).isNotEqualTo(otherConversation);
            assertThat(firstTurn).isEqualTo(sameTurn).hasSameHashCodeAs(sameTurn);
            assertThat(firstTurn).isNotEqualTo(otherTurn);
        }

        @Test
        void turnPreparationIsNotPublicOutsideConversationAggregate() throws NoSuchMethodException {
            assertThat(Modifier.isPublic(AgentTurn.class
                    .getDeclaredMethod("prepare", TurnId.class, ConversationId.class)
                    .getModifiers())).isFalse();
        }

        private AgentTurn preparedTurn() {
            return Conversation.create(AgentOwnerId.from("user-42"))
                    .prepareTurn(TurnId.from(UUID.randomUUID()));
        }
    }
}
