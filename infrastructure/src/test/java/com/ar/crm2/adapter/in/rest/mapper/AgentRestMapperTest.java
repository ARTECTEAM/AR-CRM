package com.ar.crm2.adapter.in.rest.mapper;

import com.ar.crm2.application.security.ActorContext;
import com.ar.crm2.model.agent.entity.AgentTurn;
import com.ar.crm2.model.agent.enums.TurnState;
import com.ar.crm2.model.agent.vo.AcceptedUserTurn;
import com.ar.crm2.model.agent.vo.ConversationId;
import com.ar.crm2.model.agent.vo.TurnId;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRestMapperTest {

    @Test
    void completeCommandPreservesDistinctTrustedUsuarioAndOptionalSuperUsuarioClaims() {
        UUID usuarioId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID superUsuarioId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        ActorContext privileged = new ActorContext(
                "subject", "user", null, Optional.of(usuarioId), Optional.of(superUsuarioId), Set.of());
        ActorContext normal = new ActorContext(
                "subject", "user", null, Optional.of(usuarioId), Optional.empty(), Set.of());
        AcceptedUserTurn accepted = new AcceptedUserTurn(
                AgentTurn.reconstitute(TurnId.create(), ConversationId.create(), TurnState.PREPARED), "handle");

        var privilegedCommand = AgentRestMapper.toCompleteTurnCommand(
                privileged, usuarioId, accepted, "prompt", 20);
        var normalCommand = AgentRestMapper.toCompleteTurnCommand(
                normal, usuarioId, accepted, "prompt", 20);

        assertThat(privilegedCommand.actorUsuarioId()).isEqualTo(usuarioId);
        assertThat(privilegedCommand.actorSuperUsuarioId()).isEqualTo(superUsuarioId);
        assertThat(normalCommand.actorUsuarioId()).isEqualTo(usuarioId);
        assertThat(normalCommand.actorSuperUsuarioId()).isNull();
    }
}
