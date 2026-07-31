package com.ar.crm2.config;

import com.ar.crm2.adapter.out.ai.tool.SpringAiCrmTools;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.application.trato.port.in.CambiarEstadoTratoUseCase;
import com.ar.crm2.config.testing.CapturingChatModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Narrow no-network Spring context wiring proof for the production
 * {@link AgentConfig} bean.
 *
 * <p>Wires the production {@code AgentConfig} together with a
 * deterministic {@code @Bean ChatModel openAiChatModel()} — naming
 * the bean exactly the way
 * {@code OpenAiChatAutoConfiguration#openAiChatModel} would — and a
 * deterministic {@code @Bean SpringAiCrmTools springAiCrmTools()}
 * shared bean (no per-invocation binder). The configured
 * {@link ChatClient} is round-tripped through the real Spring AI
 * request path with the owned production defaultSystem template.
 *
 * <p>No network, no credentials, and no full Spring Boot context are
 * involved: the test uses a focused
 * {@link AnnotationConfigApplicationContext} with {@link Import} on the
 * production {@link AgentConfig}.
 */
class AgentConfigOpenAiWiringTest {

    @Configuration
    @Import(AgentConfig.class)
    static class OpenAiTestContext {

        @Bean
        ChatModel openAiChatModel() {
            return new CapturingChatModel("ok-from-context");
        }

        @Bean
        SpringAiCrmTools springAiCrmTools() {
            return new SpringAiCrmTools(
                    mock(GetAllContactosUseCase.class),
                    mock(CreateContactoUseCase.class),
                    mock(CambiarEstadoTratoUseCase.class),
                    new ObjectMapper());
        }
    }

    @Test
    void qualifiedOpenAiChatModelBeanResolvesIntoExactlyOneChatClient() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                OpenAiTestContext.class)) {

            String[] chatClientBeans = context.getBeanNamesForType(ChatClient.class);
            String[] chatModelBeans = context.getBeanNamesForType(ChatModel.class);

            assertThat(chatClientBeans)
                    .as("AgentConfig must produce exactly one ChatClient bean in the wired context")
                    .hasSize(1);
            assertThat(chatModelBeans)
                    .as("the test context must expose exactly one ChatModel bean (the qualified OpenAI one)")
                    .hasSize(1);

            assertThat(chatModelBeans[0])
                    .as("ChatModel bean name must match the OpenAI autoconfig qualifier")
                    .isEqualTo("openAiChatModel");

            ChatClient chatClient = context.getBean(chatClientBeans[0], ChatClient.class);

            String content = chatClient.prompt()
                    .system(spec -> spec.param("durable_memories", "- wired-memory"))
                    .user("hi-from-context")
                    .call()
                    .content();

            assertThat(content)
                    .as("the round-trip must return the fixed CapturingChatModel answer")
                    .isEqualTo("ok-from-context");
        }
    }

    @Test
    void qualifiedOpenAiChatModelBackendIsResolvedByAgentConfigFactoryQualifier() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                OpenAiTestContext.class)) {

            ChatClient chatClient = context.getBean(ChatClient.class);

            chatClient.prompt()
                    .system(spec -> spec.param("durable_memories", "- wired-memory-2"))
                    .user("hello")
                    .call()
                    .content();

            CapturingChatModel backend = (CapturingChatModel) context.getBean("openAiChatModel");
            assertThat(backend.capturedPrompt())
                    .as("the @Qualifier(\"openAiChatModel\") ChatModel must be the OpenAI backend the agent uses")
                    .isNotNull();
            assertThat(backend.capturedPrompt().getInstructions())
                    .as("the rendered prompt must include the substituted durable memory bullet")
                    .anyMatch(instruction -> instruction.getText().contains("- wired-memory-2"));
        }
    }

    @Test
    void noDuplicateChatClientBeansAreExposedByTheOpenAiWiring() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                OpenAiTestContext.class)) {

            List<String> chatClientBeanNames = Arrays.asList(context.getBeanNamesForType(ChatClient.class));
            assertThat(chatClientBeanNames)
                    .as("AgentConfig must expose exactly one ChatClient; no aliases or "
                            + "secondary clients from the OpenAI wiring")
                    .hasSize(1)
                    .doesNotHaveDuplicates();

            long producerClientCount = chatClientBeanNames.stream()
                    .filter(name -> "chatClient".equals(name))
                    .count();
            assertThat(producerClientCount)
                    .as("AgentConfig's @Bean(name = \"chatClient\") must be the sole ChatClient in the wired context")
                    .isEqualTo(1L);
        }
    }

    @Test
    void sharedSpringAiCrmToolsBeanIsResolvedAsDefaultTools() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                OpenAiTestContext.class)) {

            SpringAiCrmTools tools = context.getBean(SpringAiCrmTools.class);
            assertThat(tools)
                    .as("WiringConfig must expose the shared SpringAiCrmTools bean")
                    .isNotNull();

            ChatClient chatClient = context.getBean(ChatClient.class);

            // Round-trip via the configured ChatClient — the shared tools
            // are registered as defaultTools, so the three allowlisted
            // tool names appear in the captured prompt rendered to the
            // CapturingChatModel.
            chatClient.prompt()
                    .system(spec -> spec.param("durable_memories", ""))
                    .user("hi")
                    .call()
                    .content();

            String systemText = ((CapturingChatModel) context.getBean("openAiChatModel"))
                    .capturedPrompt().getInstructions().get(0).getText();
            assertThat(systemText)
                    .as("the shared tools must be advertised through the configured ChatClient")
                    .contains("find_contacts")
                    .contains("create_contact")
                    .contains("update_deal_stage");
        }
    }

    @Test
    void wiredContextClosesCleanlyAfterOpenAiChatClientRoundTrip() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                OpenAiTestContext.class)) {
            ChatClient chatClient = context.getBean(ChatClient.class);
            chatClient.prompt()
                    .system(spec -> spec.param("durable_memories", "- x"))
                    .user("y")
                    .call()
                    .content();
            assertThat(AgentConfig.class)
                    .as("AgentConfig must exist on the classpath")
                    .isNotNull();

            Qualifier qualifierOnParameter = Arrays.stream(AgentConfig.class.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(
                            org.springframework.context.annotation.Bean.class))
                    .filter(method -> method.getReturnType() == ChatClient.class)
                    .flatMap(method -> Arrays.stream(method.getParameters()))
                    .map(parameter -> parameter.getAnnotation(Qualifier.class))
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "AgentConfig.chatClient must declare @Qualifier(\"openAiChatModel\")"));
            assertThat(qualifierOnParameter.value())
                    .as("@Qualifier value must match the OpenAI bean name")
                    .isEqualTo("openAiChatModel");
        }
    }
}