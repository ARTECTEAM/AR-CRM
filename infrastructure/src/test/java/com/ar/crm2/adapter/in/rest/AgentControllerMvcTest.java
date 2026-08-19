package com.ar.crm2.adapter.in.rest;

import com.ar.crm2.application.agent.turn.command.CompleteUserTurnCommand;
import com.ar.crm2.application.agent.turn.command.CreateUserTurnCommand;
import com.ar.crm2.application.agent.turn.exception.IdempotencyKeyReusedException;
import com.ar.crm2.application.agent.turn.port.in.CompleteUserTurnUseCase;
import com.ar.crm2.application.agent.turn.port.in.CreateUserTurnUseCase;
import com.ar.crm2.application.security.ActorContext;
import com.ar.crm2.application.security.exception.AuthenticatedUsuarioRequiredException;
import com.ar.crm2.model.agent.entity.AgentTurn;
import com.ar.crm2.model.agent.enums.TurnState;
import com.ar.crm2.model.agent.vo.AcceptedUserTurn;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.security.ActorContextFilterConfiguration;
import com.ar.crm2.security.ActorContextRequestAttributeFilter;
import com.ar.crm2.security.CorsConfig;
import com.ar.crm2.security.KeycloakJwtActorContextMapper;
import com.ar.crm2.security.KeycloakJwtAuthoritiesConverter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Focused MVC slice for the Pipely CRM conversational ingress.
 *
 * <p>The servlet filter chain is disabled; a trusted {@link ActorContext}
 * is injected directly into the request attribute the controller reads.
 * The OAuth2 Resource Server chain is exercised in {@code SecurityConfigTest}
 * and via the end-to-end {@code AgentConversationIT}. An anonymous request
 * is rejected by the controller mapper (defense-in-depth → 403), proving
 * the controller never accepts a request without a trusted identity.
 */
@WebMvcTest(controllers = {AgentController.class, GlobalExceptionHandler.class})
@Import({
        CorsConfig.class,
        ActorContextFilterConfiguration.class, ActorContextRequestAttributeFilter.class,
        KeycloakJwtActorContextMapper.class, KeycloakJwtAuthoritiesConverter.class,
        AgentControllerMvcTest.TestConfig.class
})
@AutoConfigureMockMvc(addFilters = false)
class AgentControllerMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CreateUserTurnUseCase createUserTurnUseCase;
    @MockitoBean private CompleteUserTurnUseCase completeUserTurnUseCase;

    @Test
    void anonymousRequest_returns403() throws Exception {
        mockMvc.perform(post("/api/agent/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\",\"idempotencyKey\":\"k1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("authenticated")));
        verify(createUserTurnUseCase, never()).create(any());
        verify(completeUserTurnUseCase, never()).complete(any());
    }

    @Test
    void authenticatedRequest_returns200AndCapturesActorIdentity() throws Exception {
        AcceptedUserTurn accepted = stubAcceptedTurn();
        when(createUserTurnUseCase.create(any())).thenReturn(accepted);
        when(completeUserTurnUseCase.complete(any())).thenReturn("the-final-content");

        UUID actorUsuarioId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        mockMvc.perform(authenticatedPost("alice-subject", actorUsuarioId, "hello", "k1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("the-final-content"));

        ArgumentCaptor<CreateUserTurnCommand> createCaptor = ArgumentCaptor.forClass(CreateUserTurnCommand.class);
        verify(createUserTurnUseCase, times(1)).create(createCaptor.capture());
        assertThat(createCaptor.getValue().actorSubject())
                .as("actor subject MUST come from the trusted ActorContext, not from the body")
                .isEqualTo("alice-subject");
        assertThat(createCaptor.getValue().prompt()).isEqualTo("hello");
        assertThat(createCaptor.getValue().idempotencyKey()).isEqualTo("k1");

        ArgumentCaptor<CompleteUserTurnCommand> completeCaptor = ArgumentCaptor.forClass(CompleteUserTurnCommand.class);
        verify(completeUserTurnUseCase, times(1)).complete(completeCaptor.capture());
        assertThat(completeCaptor.getValue().actorSubject()).isEqualTo("alice-subject");
        assertThat(completeCaptor.getValue().actorUsuarioId())
                .as("actorUsuarioId MUST come from the trusted ActorContext (JWT usuario_id claim)")
                .isEqualTo(actorUsuarioId);
    }

    @Test
    void differentActors_yieldDifferentActorSubjects() throws Exception {
        AcceptedUserTurn accepted = stubAcceptedTurn();
        when(createUserTurnUseCase.create(any())).thenReturn(accepted);
        when(completeUserTurnUseCase.complete(any())).thenReturn("ok");

        mockMvc.perform(authenticatedPost("bob-subject",
                                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                                "hi", "k1"))
                .andExpect(status().isOk());

        ArgumentCaptor<CreateUserTurnCommand> captor = ArgumentCaptor.forClass(CreateUserTurnCommand.class);
        verify(createUserTurnUseCase, times(1)).create(captor.capture());
        assertThat(captor.getValue().actorSubject()).isEqualTo("bob-subject");
    }

    @Test
    void actorWithoutUsuarioId_returns403() throws Exception {
        when(createUserTurnUseCase.create(any()))
                .thenThrow(AuthenticatedUsuarioRequiredException.forMissingUsuarioId());

        mockMvc.perform(authenticatedPost("subject-without-usuario", null, "hi", "k1"))
                .andExpect(status().isForbidden());
        verify(completeUserTurnUseCase, never()).complete(any());
    }

    @Test
    void changedMessageWithReusedKey_returns409() throws Exception {
        when(createUserTurnUseCase.create(any())).thenThrow(new IdempotencyKeyReusedException());

        mockMvc.perform(authenticatedPost("alice-subject",
                                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                                "changed-content", "shared-key"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("idempotencyKey")))
                .andExpect(jsonPath("$.error", containsString("reused")));

        verify(createUserTurnUseCase, times(1)).create(any());
        verify(completeUserTurnUseCase, never()).complete(any());
    }

    @Test
    void blankMessage_returns400() throws Exception {
        mockMvc.perform(authenticatedPost("alice-subject",
                                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                                "   ", "k1"))
                .andExpect(status().isBadRequest());
        verify(createUserTurnUseCase, never()).create(any());
    }

    @Test
    void blankIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(authenticatedPost("alice-subject",
                                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                                "hi", "   "))
                .andExpect(status().isBadRequest());
        verify(createUserTurnUseCase, never()).create(any());
    }

    @Test
    void getAgentMessages_returns405() throws Exception {
        mockMvc.perform(get("/api/agent/messages")
                        .with(trustedActor("alice-subject",
                                UUID.fromString("11111111-1111-1111-1111-111111111111"))))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void postAgentCompletion_returns404() throws Exception {
        mockMvc.perform(post("/api/agent/completion")
                        .with(trustedActor("alice-subject",
                                UUID.fromString("11111111-1111-1111-1111-111111111111")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"x\",\"idempotencyKey\":\"k\"}"))
                .andExpect(status().isNotFound());
        verify(createUserTurnUseCase, never()).create(any());
    }

    @Test
    void postAgentRegenerate_returns404() throws Exception {
        mockMvc.perform(post("/api/agent/regenerate")
                        .with(trustedActor("alice-subject",
                                UUID.fromString("11111111-1111-1111-1111-111111111111")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"x\",\"idempotencyKey\":\"k\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postToolFindContacts_returns404() throws Exception {
        mockMvc.perform(post("/api/agent/tools/find_contacts")
                        .with(trustedActor("alice-subject",
                                UUID.fromString("11111111-1111-1111-1111-111111111111")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    /** Seeds a trusted {@link ActorContext} into the request attribute the controller reads. */
    private static org.springframework.test.web.servlet.request.RequestPostProcessor trustedActor(
            String subject, UUID usuarioId) {
        Optional<UUID> usuarioIdOpt = usuarioId == null ? Optional.empty() : Optional.of(usuarioId);
        ActorContext actor = new ActorContext(
                subject, subject, subject + "@example.com",
                usuarioIdOpt, Optional.empty(), Set.of("USER"));
        return request -> {
            request.setAttribute(ActorContextRequestAttributeFilter.ACTOR_CONTEXT_ATTRIBUTE, actor);
            return request;
        };
    }

    private static MockHttpServletRequestBuilder authenticatedPost(
            String subject, UUID usuarioId, String message, String idempotencyKey) {
        return post("/api/agent/messages")
                .with(trustedActor(subject, usuarioId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"message\":\"%s\",\"idempotencyKey\":\"%s\"}",
                        message.replace("\\", "\\\\").replace("\"", "\\\""),
                        idempotencyKey.replace("\\", "\\\\").replace("\"", "\\\"")));
    }

    private static AcceptedUserTurn stubAcceptedTurn() {
        AgentTurn turn = org.mockito.Mockito.mock(AgentTurn.class);
        org.mockito.Mockito.when(turn.getId()).thenReturn(TurnId.create());
        org.mockito.Mockito.when(turn.getState()).thenReturn(TurnState.PREPARED);
        return new AcceptedUserTurn(turn, "opaque-handle-test");
    }

    /** Bindings required by SecurityConfig filters; not relevant to agent ingress. */
    @TestConfiguration
    static class TestConfig {
        @Bean org.springframework.web.servlet.config.annotation.WebMvcConfigurer jacksonConverterConfigurer() {
            return new org.springframework.web.servlet.config.annotation.WebMvcConfigurer() {
                @Override
                public void extendMessageConverters(
                        java.util.List<org.springframework.http.converter.HttpMessageConverter<?>> converters) {
                    converters.add(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter());
                }
            };
        }
    }
}
