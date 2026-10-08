package com.ar.crm2.config;

import com.ar.crm2.BootApplication;
import com.ar.crm2.application.contacto.command.GetAllContactosCommand;
import com.ar.crm2.application.contacto.port.in.SearchContactosForActorUseCase;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the authenticated agent endpoint through the real local-role and tool-completion composition. */
@SpringBootTest(
        classes = BootApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:agent-capability-http;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "spring.sql.init.mode=never",
                "spring.ai.openai.api-key=offline-test-key",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/test-issuer",
                "crm2.authorization.bootstrap-admin-subject="
        })
@AutoConfigureMockMvc
@Import(AgentAuthorizationHttpCompositionTest.TestJwtConfiguration.class)
class AgentAuthorizationHttpCompositionTest {
    private static final String SUBJECT = "agent-local-subject";
    private static final UUID LOCAL_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FORGED_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ROLE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtEncoder jwtEncoder;

    @MockitoBean(name = "openAiChatModel") private ChatModel chatModel;
    @MockitoBean private SearchContactosForActorUseCase searchContactosForActorUseCase;

    private final List<Set<String>> callbacksByModelRequest = new CopyOnWriteArrayList<>();
    private final AtomicBoolean searchToolRequested = new AtomicBoolean();

    @BeforeEach
    void seedActiveDatabaseRoleAndModel() {
        jdbc.update("DELETE FROM rol_permisos");
        jdbc.update("DELETE FROM usuarios");
        jdbc.update("DELETE FROM roles");
        jdbc.update("INSERT INTO roles (id, nombre, descripcion, activo) VALUES (?, ?, ?, ?)",
                ROLE_ID.toString(), "Agent test role", "Local role for HTTP authorization test", true);
        insertReadGrant("CONTACTO");
        insertReadGrant("EMPRESA");
        jdbc.update("INSERT INTO usuarios (id, nombre, correo, rol_id, creado_en, activo, keycloak_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                LOCAL_USER_ID.toString(), "Local Agent", "agent@example.test", ROLE_ID.toString(),
                java.time.LocalDateTime.now(), true, SUBJECT);

        callbacksByModelRequest.clear();
        searchToolRequested.set(false);
        when(searchContactosForActorUseCase.search(any())).thenReturn(List.of());
        when(chatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        when(chatModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            Set<String> available = callbackNames(prompt);
            callbacksByModelRequest.add(available);
            if (available.contains("find_contacts") && searchToolRequested.compareAndSet(false, true)) {
                AssistantMessage toolCall = AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "local-search-call", "function", "find_contacts", "{}")))
                        .build();
                return new ChatResponse(List.of(new Generation(toolCall)));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("deterministic-response"))));
        });
    }

    @Test
    void validatedJwtUsesActiveLocalRoleForCallbacksAndRefreshesAfterRoleRevocation() throws Exception {
        String bearer = signedBearerToken();

        mockMvc.perform(post("/api/agent/messages")
                        .header("Authorization", "Bearer " + bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"find contacts\",\"idempotencyKey\":\"role-turn-1\","
                                + "\"actorUsuarioId\":\"" + FORGED_USER_ID + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("deterministic-response"));

        assertThat(callbacksByModelRequest).hasSize(2);
        assertThat(callbacksByModelRequest.getFirst()).contains("find_contacts");
        ArgumentCaptorSupport.verifyTrustedActor(searchContactosForActorUseCase, LOCAL_USER_ID);

        jdbc.update("DELETE FROM rol_permisos WHERE rol_id = ? AND recurso = ?", ROLE_ID.toString(), "EMPRESA");

        mockMvc.perform(post("/api/agent/messages")
                        .header("Authorization", "Bearer " + bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"find contacts again\",\"idempotencyKey\":\"role-turn-2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("deterministic-response"));

        assertThat(callbacksByModelRequest).hasSize(3);
        assertThat(callbacksByModelRequest.get(2)).doesNotContain("find_contacts");
        verify(searchContactosForActorUseCase, times(1)).search(any(GetAllContactosCommand.class));
    }

    private void insertReadGrant(String resource) {
        jdbc.update("INSERT INTO rol_permisos "
                        + "(recurso, alcance, grupos_escritura, grupos_lectura, acciones, ids_permitidos, rol_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                resource, "TODO_COMPARTIDO", "", "", "LEER", "", ROLE_ID.toString());
    }

    private String signedBearerToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://localhost/test-issuer")
                .subject(SUBJECT)
                .issuedAt(now.minusSeconds(5))
                .expiresAt(now.plusSeconds(120))
                .claim("usuario_id", FORGED_USER_ID.toString())
                .claim("realm_access", java.util.Map.of("roles", List.of("USER")))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId(TestJwtConfiguration.KEY_ID).build(), claims))
                .getTokenValue();
    }

    private static Set<String> callbackNames(Prompt prompt) {
        if (!(prompt.getOptions() instanceof ToolCallingChatOptions options) || options.getToolCallbacks() == null) {
            return Set.of();
        }
        return options.getToolCallbacks().stream()
                .map(ToolCallback::getToolDefinition)
                .map(definition -> definition.name())
                .collect(Collectors.toUnmodifiableSet());
    }

    @TestConfiguration
    static class TestJwtConfiguration {
        private static final String KEY_ID = "agent-http-test-key";
        private static final RSAKey TEST_KEY = generateKey();

        @Bean
        JwtEncoder jwtEncoder() {
            return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(TEST_KEY)));
        }

        @Bean
        JwtDecoder jwtDecoder() throws Exception {
            return NimbusJwtDecoder.withPublicKey((RSAPublicKey) TEST_KEY.toRSAPublicKey())
                    .signatureAlgorithm(SignatureAlgorithm.RS256)
                    .build();
        }

        private static RSAKey generateKey() {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair pair = generator.generateKeyPair();
                return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                        .privateKey((RSAPrivateKey) pair.getPrivate())
                        .keyID(KEY_ID)
                        .build();
            } catch (Exception failure) {
                throw new ExceptionInInitializerError(failure);
            }
        }
    }

    private static final class ArgumentCaptorSupport {
        private static void verifyTrustedActor(SearchContactosForActorUseCase useCase, UUID expectedActor) {
            org.mockito.ArgumentCaptor<GetAllContactosCommand> captor =
                    org.mockito.ArgumentCaptor.forClass(GetAllContactosCommand.class);
            verify(useCase, times(1)).search(captor.capture());
            assertThat(captor.getValue().actorUsuarioId())
                    .as("trusted actor must come from JWT subject to active local user lookup, not usuario_id claim")
                    .isEqualTo(expectedActor)
                    .isNotEqualTo(FORGED_USER_ID);
        }
    }
}