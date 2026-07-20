package com.ar.crm2.model.agent.vo;

import com.ar.crm2.exception.InvariantViolationException;
import com.ar.crm2.model.agent.entity.AgentTurn;
import com.ar.crm2.model.agent.entity.Conversation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcceptedUserTurnTest {

    @Test
    void acceptsTheCanonicalPreparedTurnAndRedactsItsOpaqueHandle() {
        AgentTurn turn = preparedTurn();
        String opaqueHandle = UUID.randomUUID().toString();

        AcceptedUserTurn receipt = new AcceptedUserTurn(turn, opaqueHandle);

        assertThat(receipt.turn()).isSameAs(turn);
        assertThat(receipt.opaqueHandle()).isEqualTo(opaqueHandle);
        assertThat(receipt.toString()).doesNotContain(opaqueHandle).contains("[REDACTED]");
    }

    @Test
    void rejectsMissingReceiptValuesAndUsesBothValuesForEquality() {
        AgentTurn turn = preparedTurn();
        String opaqueHandle = UUID.randomUUID().toString();

        assertThatThrownBy(() -> new AcceptedUserTurn(null, opaqueHandle))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> new AcceptedUserTurn(turn, "  "))
                .isInstanceOf(InvariantViolationException.class);
        assertThat(new AcceptedUserTurn(turn, opaqueHandle))
                .isEqualTo(new AcceptedUserTurn(turn, opaqueHandle))
                .isNotEqualTo(new AcceptedUserTurn(turn, UUID.randomUUID().toString()));
    }

    private AgentTurn preparedTurn() {
        return Conversation.create(AgentOwnerId.from("owner-a"))
                .createTurn(TurnId.from(UUID.randomUUID()));
    }
}
