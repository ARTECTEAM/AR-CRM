package com.ar.crm2.adapter.in.rest;

import com.ar.crm2.adapter.out.ai.tool.SpringAiCrmTools;
import com.ar.crm2.adapter.out.persistence.agent.AgentTurnAdapter;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentConversationRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentTurnRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentTurnRequestRepository;
import com.ar.crm2.adapter.out.persistence.agent.repository.AgentVisibleHistoryRepository;
import com.ar.crm2.application.agent.turn.port.out.FindEligibleDurableMemoriesPort;
import com.ar.crm2.application.agent.turn.port.in.CompleteUserTurnUseCase;
import com.ar.crm2.application.agent.turn.port.in.CreateUserTurnUseCase;
import com.ar.crm2.application.agent.turn.port.out.ChatCompletionPort;
import com.ar.crm2.application.agent.turn.port.out.CompletePreparedTurnPort;
import com.ar.crm2.application.agent.turn.port.out.CreateUserTurnPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedAssistantContentPort;
import com.ar.crm2.application.agent.turn.port.out.FindCompletedVisibleHistoryPort;
import com.ar.crm2.application.agent.turn.service.CompleteUserTurnService;
import com.ar.crm2.application.agent.turn.service.CreateUserTurnService;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.application.contacto.port.out.SearchContactosPort;
import com.ar.crm2.application.agent.tool.port.in.AgentCrmWriteUseCase;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.agent.vo.VisibleMessage;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.UsuarioId;
import com.ar.crm2.security.ActorContextFilterConfiguration;
import com.ar.crm2.security.ActorContextRequestAttributeFilter;
import com.ar.crm2.security.BotApiTokenFilter;
import com.ar.crm2.security.CorsConfig;
import com.ar.crm2.security.KeycloakJwtActorContextMapper;
import com.ar.crm2.security.KeycloakJwtAuthoritiesConverter;
import com.ar.crm2.security.SecurityConfig;
import com.ar.crm2.security.WaApiKeyFilter;
import com.ar.crm2.security.WaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/** H2 + real security chain, persistence, owner isolation, and no-network callback execution proof. */
@SpringBootTest(classes = AgentConversationIT.AgentHarness.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:agent-conv-it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
class AgentConversationIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private AgentConversationRepository conversationRepository;
    @Autowired private AgentTurnRepository turnRepository;
    @Autowired private AgentTurnRequestRepository requestRepository;
    @Autowired private AgentVisibleHistoryRepository historyRepository;
    @Autowired private StubChatCompletionPort stubCompletion;

    @AfterEach
    void resetState() {
        historyRepository.deleteAll();
        requestRepository.deleteAll();
        turnRepository.deleteAll();
        conversationRepository.deleteAll();
        stubCompletion.reset();
    }

    @Test
    void anonymous_isRejectedByRealSecurityChain_beforeAnyUseCaseRuns() throws Exception {
        mockMvc.perform(post("/api/agent/messages")
                        .contentType("application/json")
                        .content("{\"message\":\"hi\",\"idempotencyKey\":\"k1\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(conversationRepository.count()).isZero();
    }

    @Test
    void singleRequest_producesRoleBearingHistoryInChronologicalOrder() throws Exception {
        mockMvc.perform(agentPost("subject-it-alice", "{\"message\":\"hi\",\"idempotencyKey\":\"k1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(CANONICAL));
        assertThat(conversationRepository.count()).isEqualTo(1L);
        assertThat(turnRepository.count()).isEqualTo(1L);
        assertThat(requestRepository.count()).isEqualTo(1L);
        assertThat(historyRepository.count()).isEqualTo(2L);
        // USER then ASSISTANT in chronological order is the design-required
        // role-bearing visible history. The repository persists ascending
        // visibleAt; the first row is the user prompt, the second is the
        // assistant final content.
        List<String> rolesInOrder = historyRepository.findAll(Sort.by("visibleAt"))
                .stream().map(h -> h.getRole()).toList();
        assertThat(rolesInOrder).containsExactly("USER", "ASSISTANT");
    }

    @Test
    void idempotentRetryConvergesToOneUserOneAssistantOneResponse() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(agentPost("subject-it-alice", "{\"message\":\"hi\",\"idempotencyKey\":\"k1\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value(CANONICAL));
        }
        assertThat(conversationRepository.count()).isEqualTo(1L);
        assertThat(turnRepository.count()).isEqualTo(1L);
        assertThat(requestRepository.count()).isEqualTo(1L);
        assertThat(historyRepository.count()).isEqualTo(2L);
    }

    @Test
    void retryConvergesBySkippingTheChatCompletionPortAfterFirstCompletion() throws Exception {
        mockMvc.perform(agentPost("subject-it-alice", "{\"message\":\"first\",\"idempotencyKey\":\"r1\"}"))
                .andExpect(status().isOk());
        int callsAfterFirst = stubCompletion.callCount();
        mockMvc.perform(agentPost("subject-it-alice", "{\"message\":\"first\",\"idempotencyKey\":\"r1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(CANONICAL));
        assertThat(stubCompletion.callCount())
                .as("replay serves the converged canonical content; ChatCompletionPort is NOT invoked again")
                .isEqualTo(callsAfterFirst);
    }

    @Test
    void differentOwnersAreIsolated_andDistinctTrustedActorSubjects() throws Exception {
        mockMvc.perform(agentPost("subject-it-owner-a", "{\"message\":\"shared\",\"idempotencyKey\":\"shared\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(agentPost("subject-it-owner-b", "{\"message\":\"shared\",\"idempotencyKey\":\"shared\"}"))
                .andExpect(status().isOk());
        assertThat(conversationRepository.count()).isEqualTo(2L);
        assertThat(turnRepository.count()).isEqualTo(2L);
        assertThat(requestRepository.count()).isEqualTo(2L);
        assertThat(historyRepository.count()).isEqualTo(4L);
        assertThat(stubCompletion.lastOwnerIds())
                .as("two distinct JWT subjects must produce two distinct trusted AgentOwnerIds")
                .containsExactly("subject-it-owner-a", "subject-it-owner-b");
    }

    @Test
    void completionPort_receivesBoundedHistoryEligibleMemoryAndThreeAllowlistedTools() throws Exception {
        mockMvc.perform(agentPost("subject-it-bounded", "{\"message\":\"x\",\"idempotencyKey\":\"b1\"}"))
                .andExpect(status().isOk());
        assertThat(stubCompletion.lastHistorySize())
                .as("bounded history is bounded by the visible history limit")
                .isLessThanOrEqualTo(20);
        assertThat(stubCompletion.lastMemories()).isNotEmpty();
        assertThat(stubCompletion.lastToolResult())
                .as("authenticated conversation must execute the shared allowlisted callback")
                .isEqualTo("{\"contacts\":[]}");
        assertThat(stubCompletion.lastResponse())
                .as("the callback result must reach the final conversational response")
                .isEqualTo("final-after-find_contacts:{\"contacts\":[]}");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder agentPost(
            String subject, String body) {
        return post("/api/agent/messages")
                .header("Authorization", "Bearer " + subjectToToken(subject))
                .contentType("application/json").content(body);
    }

    private static String subjectToToken(String subject) {
        return subject + "-token";
    }

    private static final String CANONICAL = "the-only-canonical-response";

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.ar.crm2.adapter.out.persistence.agent.entity")
    @EnableJpaRepositories(basePackages = "com.ar.crm2.adapter.out.persistence.agent.repository")
    @Import({AgentController.class, GlobalExceptionHandler.class, SecurityConfig.class,
            CorsConfig.class, ActorContextFilterConfiguration.class, ActorContextRequestAttributeFilter.class,
            KeycloakJwtActorContextMapper.class, KeycloakJwtAuthoritiesConverter.class,
            AgentConversationIT.HarnessBeans.class})
    static class AgentHarness {}

    @Configuration
    static class HarnessBeans {
        @Bean AgentTurnAdapter adapter(AgentConversationRepository c, AgentTurnRepository t,
                AgentTurnRequestRepository r, AgentVisibleHistoryRepository h) {
            return new AgentTurnAdapter(c, t, r, h);
        }
        @Bean ChatCompletionPort chatCompletionPort(SpringAiCrmTools tools) {
            return new StubChatCompletionPort(tools);
        }
        @Bean FindEligibleDurableMemoriesPort findEligibleDurableMemoriesPort() {
            return ownerId -> List.of("Treat customer X carefully");
        }
        @Bean SearchContactosPort searchContactosPort() {
            return (u, s, e, emp, r, c, m) -> List.of();
        }
        @Bean GetAllContactosUseCase getAllContactosUseCase() { return cmd -> List.of(); }
        @Bean CreateContactoUseCase createContactoUseCase() { return cmd -> null; }
        @Bean AgentCrmWriteUseCase agentCrmWriteUseCase() {
            // C1 stub: the IT exercises the conversational endpoint, not the
            // write orchestrator. The bounded output mapper tolerates a null
            // Trato, so a no-op use case is sufficient.
            return command -> null;
        }
        @Bean SpringAiCrmTools crmTools(GetAllContactosUseCase get, CreateContactoUseCase create,
                AgentCrmWriteUseCase agentCrmWriteUseCase, ObjectMapper om) {
            return new SpringAiCrmTools(get, create, agentCrmWriteUseCase, om);
        }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean WaProperties waProperties() { return new WaProperties("test-key", null); }
        @Bean WaApiKeyFilter waApiKeyFilter(WaProperties p) { return new WaApiKeyFilter(p); }
        @Bean com.ar.crm2.whatsapp.application.bot.port.in.FindBotByTokenUseCase findBotByTokenUseCase() {
            return token -> Optional.empty();
        }
        @Bean BotApiTokenFilter botApiTokenFilter(
                com.ar.crm2.whatsapp.application.bot.port.in.FindBotByTokenUseCase find) {
            return new BotApiTokenFilter(find);
        }
        @Bean CreateUserTurnService createUserTurnService(CreateUserTurnPort p) { return new CreateUserTurnService(p); }
        @Bean CompleteUserTurnService completeUserTurnService(FindCompletedAssistantContentPort fa,
                FindCompletedVisibleHistoryPort fh, FindEligibleDurableMemoriesPort fm,
                CompletePreparedTurnPort cp, ChatCompletionPort cc) {
            return new CompleteUserTurnService(fa, fh, fm, cp, cc);
        }
        @Bean JwtDecoder jwtDecoder() {
            return token -> {
                String raw = token.toString();
                String subject = raw.endsWith("-token") ? raw.substring(0, raw.length() - "-token".length()) : raw;
                return Jwt.withTokenValue(token).headers(h -> h.putAll(Map.of("alg", "RS256"))).claims(c -> {
                    c.put("sub", subject);
                    c.put("preferred_username", subject);
                    c.put("email", subject + "@example.com");
                    c.put("usuario_id", UUID.randomUUID().toString());
                    c.put("aud", List.of("crm2-api"));
                }).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
                  .issuer("http://localhost:8180/realms/crm2-local").build();
            };
        }
    }

    /** Records completion inputs while executing the shared allowlisted callback without network access. */
    static final class StubChatCompletionPort implements ChatCompletionPort {
        private final org.springframework.ai.tool.ToolCallback findContacts;
        private final AtomicInteger calls = new AtomicInteger();
        private final List<String> ownerIds = new java.util.ArrayList<>();
        private List<VisibleMessage> lastHistory = List.of();
        private List<String> lastMemories = List.of();
        private String lastToolResult;
        private String lastResponse;

        StubChatCompletionPort(SpringAiCrmTools tools) {
            findContacts = Arrays.stream(ToolCallbacks.from(tools))
                    .filter(tool -> "find_contacts".equals(tool.getToolDefinition().name()))
                    .findFirst().orElseThrow();
        }
        @Override public String complete(AgentOwnerId ownerId, UUID actorUsuarioId, TurnId turnId,
                List<VisibleMessage> visibleHistory, List<String> durableMemories, String normalizedPrompt) {
            calls.incrementAndGet(); lastHistory = visibleHistory; lastMemories = durableMemories;
            ownerIds.add(ownerId.value());
            lastToolResult = findContacts.call("{}", new ToolContext(Map.of("actorUsuarioId", actorUsuarioId)));
            return lastResponse = "x".equals(normalizedPrompt)
                    ? "final-after-find_contacts:" + lastToolResult : CANONICAL;
        }
        int callCount() { return calls.get(); }
        void reset() { calls.set(0); lastHistory = List.of(); lastMemories = List.of(); lastToolResult = null; lastResponse = null; ownerIds.clear(); }
        int lastHistorySize() { return lastHistory.size(); }
        List<String> lastMemories() { return lastMemories; }
        String lastToolResult() { return lastToolResult; }
        String lastResponse() { return lastResponse; }
        List<String> lastOwnerIds() { return List.copyOf(ownerIds); }
    }
}