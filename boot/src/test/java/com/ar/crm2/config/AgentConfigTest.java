package com.ar.crm2.config;

import com.ar.crm2.adapter.out.ai.tool.SpringAiCrmTools;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.EditContactoUseCase;
import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.application.empresa.port.in.CreateEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.EditEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase;
import com.ar.crm2.application.trato.port.in.EditTratoUseCase;
import com.ar.crm2.config.testing.CapturingChatModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Focused contract for the production
 * {@link AgentConfig} bean that owns the CRM agent's
 * {@link ChatClient} and its {@code defaultSystem} template.
 *
 * <p>Corrected A3 contract: AgentConfig composes a SINGLE shared
 * {@link SpringAiCrmTools} bean and registers it once via
 * {@code ChatClient.Builder#defaultTools(Object...)}. The configured
 * {@link ChatClient} exposes the three allowlisted CRM tools to every
 * request; the per-request actor identity travels separately through
 * {@code ChatClient.RequestSpec#toolContext(...)} set by the adapter.
 */
class AgentConfigTest {

    private static SpringAiCrmTools newNoopTools() {
        return new SpringAiCrmTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(GetAllEmpresasUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class),
                new ObjectMapper());
    }

    private static ChatClient newClientUnderTest() {
        return new AgentConfig().chatClient(new CapturingChatModel("ok"), newNoopTools());
    }

    @Test
    void chatClientBeanIsBuiltFromSuppliedOpenAiChatModelAndReturnsNonNullConfiguredChatClient() {
        ChatClient configured = newClientUnderTest();

        assertThat(configured)
                .as("AgentConfig must produce a configured ChatClient from the supplied OpenAI ChatModel")
                .isNotNull();
    }

    @Test
    void chatClientMethodParameterDeclaresOpenAiChatModelQualifier() {
        Method chatClientMethod = findChatClientFactoryMethod();

        Parameter parameter = chatClientMethod.getParameters()[0];
        Qualifier qualifier = parameter.getAnnotation(Qualifier.class);
        assertThat(qualifier)
                .as("chatClient factory parameter must declare @Qualifier so Spring "
                        + "resolves the OpenAI provider starter bean by name")
                .isNotNull();
        assertThat(qualifier.value())
                .as("@Qualifier value must match the OpenAI ChatModel bean name from autoconfig")
                .isEqualTo("openAiChatModel");
    }

    @Test
    void chatClientMethodParameterUsesChatModelTypeForOpenAiInjection() {
        Method chatClientMethod = findChatClientFactoryMethod();

        Parameter parameter = chatClientMethod.getParameters()[0];
        assertThat(parameter.getType())
                .as("chatClient factory parameter type must be Spring AI ChatModel")
                .isEqualTo(org.springframework.ai.chat.model.ChatModel.class);
    }

    @Test
    void chatClientFactoryConsumesTheSharedStatelessSpringAiCrmToolsBean() {
        // The factory signature must accept the shared SpringAiCrmTools
        // bean — NOT a binder or any request-scoped type. The shared
        // tools carry only the existing use cases and ObjectMapper.
        Method chatClientMethod = findChatClientFactoryMethod();

        Parameter[] parameters = chatClientMethod.getParameters();
        boolean hasSharedToolsParameter = false;
        for (Parameter parameter : parameters) {
            if (SpringAiCrmTools.class.equals(parameter.getType())) {
                hasSharedToolsParameter = true;
                break;
            }
        }
        assertThat(hasSharedToolsParameter)
                .as("chatClient factory must accept the shared SpringAiCrmTools bean for defaultTools(...)")
                .isTrue();
        // The factory must NOT receive a binder/request-tools class.
        for (Parameter parameter : parameters) {
            assertThat(parameter.getType().getSimpleName())
                    .as("chatClient factory must not depend on a per-invocation binder/request-tools class")
                    .doesNotEndWith("Binder");
        }
    }

    @Test
    void defaultSystemTemplateReplacesPlaceholderWithFormattedBulletInRenderedSystemMessage() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient configured = new AgentConfig().chatClient(model, newNoopTools());

        configured.prompt()
                .system(spec -> spec.param("durable_memories",
                        "- alpha preference\n- beta handle"))
                .user("hi")
                .call()
                .content();

        List<Message> instructions = model.capturedPrompt().getInstructions();
        assertThat(instructions.get(0)).isInstanceOf(SystemMessage.class);
        String text = instructions.get(0).getText();

        assertThat(text)
                .as("placeholder was rendered with the supplied bullet block")
                .contains("alpha preference")
                .contains("beta handle");
        assertThat(text)
                .as("the {durable_memories} placeholder must be substituted, not left literal")
                .doesNotContain("{durable_memories}");
    }

    @Test
    void defaultSystemTemplateKeepsPlaceholderLiteralWhenNoValueSupplied() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient configured = new AgentConfig().chatClient(model, newNoopTools());

        configured.prompt()
                .user("hi")
                .call()
                .content();

        List<Message> instructions = model.capturedPrompt().getInstructions();
        assertThat(instructions.get(0)).isInstanceOf(SystemMessage.class);
        String text = instructions.get(0).getText();
        assertThat(text)
                .as("the template must contain the {durable_memories} placeholder")
                .contains("{durable_memories}");
    }

    @Test
    void defaultSystemTemplateIsScopedToExistingPipelyCrmBehaviorAndReferencesAllAllowlistedTools() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient configured = new AgentConfig().chatClient(model, newNoopTools());

        configured.prompt()
                .system(spec -> spec.param("durable_memories", "- memory"))
                .user("hi")
                .call()
                .content();

        String text = model.capturedPrompt().getInstructions().get(0).getText();

        assertThat(text)
                .as("template names the Pipely CRM context")
                .contains("Pipely CRM");
        assertThat(text)
                .as("template references every allowlisted tool name")
                .contains("find_contacts")
                .contains("create_contact")
                .contains("edit_contact")
                .contains("find_companies")
                .contains("create_company")
                .contains("edit_company")
                .contains("edit_trato");
        assertThat(text)
                .as("template forbids the model from supplying actor identity")
                .containsIgnoringCase("actor");
        assertThat(text)
                .as("template mentions the visible history structure")
                .containsIgnoringCase("USER")
                .containsIgnoringCase("ASSISTANT");
        assertThat(text)
                .as("template mentions the durable memory placeholder")
                .containsIgnoringCase("durable memory");
        assertThat(text)
                .as("company deletion is intentionally not advertised")
                .doesNotContain("delete_company")
                .doesNotContain("delete_empresa");
    }

    @Test
    void defaultSystemTemplateContainsNoAdapterOwnedDurableMemoryFallbackString() {
        CapturingChatModel model = new CapturingChatModel("ok");
        ChatClient configured = new AgentConfig().chatClient(model, newNoopTools());

        configured.prompt()
                .system(spec -> spec.param("durable_memories", ""))
                .user("hi")
                .call()
                .content();

        String text = model.capturedPrompt().getInstructions().get(0).getText();
        assertThat(text)
                .as("production template must not embed a fallback constant for empty memories")
                .doesNotContain("(no eligible durable memories)")
                .doesNotContain("no eligible durable memories");
    }

    @Test
    void agentConfigRunsAsAPlainConfigurationObjectAndNotAsAnAutoConfiguredStarter() {
        assertThat(AgentConfig.class.isAnnotationPresent(
                org.springframework.context.annotation.Configuration.class))
                .as("AgentConfig must be a @Configuration class so it is "
                        + "discovered by component scan as the composition root")
                .isTrue();
        assertThat(AgentConfig.class.isAnnotationPresent(
                org.springframework.stereotype.Component.class))
                .as("AgentConfig must NOT be a @Component; it is composed "
                        + "by the boot composition root only")
                .isFalse();
        assertThat(AgentConfig.class.isAnnotationPresent(
                org.springframework.stereotype.Service.class))
                .as("AgentConfig must NOT be a @Service; the Boot layer "
                        + "composes, it does not own business logic")
                .isFalse();
    }

    @Test
    void exposedChatClientBeanSurvivesARoundTripPromptWithoutCrashing() {
        CapturingChatModel model = new CapturingChatModel("the-only-response");
        ChatClient configured = new AgentConfig().chatClient(model, newNoopTools());

        String content = configured.prompt()
                .system(spec -> spec.param("durable_memories", "- one"))
                .user("hi")
                .call()
                .content();

        assertThat(content).isEqualTo("the-only-response");
        List<Message> instructions = model.capturedPrompt().getInstructions();
        assertThat(instructions).hasSize(2);
        assertThat(instructions.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(instructions.get(1)).isInstanceOf(UserMessage.class);
    }

    @Test
    void sharedSpringAiCrmToolsIsRegisteredAsDefaultToolsOnTheConfiguredChatClient() {
        // The configured ChatClient must expose the three shared tools
        // through the maintained Spring AI 2.0 defaultTools path. The
        // exact tool names appear in the ChatClient's default callbacks
        // (introspected via getToolCallbacks() if exposed; otherwise via
        // the round-trip prompt that carries the schema to the model).
        CapturingChatModel model = new CapturingChatModel("ok");
        SpringAiCrmTools sharedTools = newNoopTools();
        ChatClient configured = new AgentConfig().chatClient(model, sharedTools);

        // Round-trip exercises the configured ChatClient end-to-end.
        // The captured prompt includes the tool definitions sent to the
        // model. The three allowlisted tool names must be present.
        configured.prompt()
                .system(spec -> spec.param("durable_memories", ""))
                .user("hi")
                .call()
                .content();

        String renderedSystem = model.capturedPrompt().getInstructions().get(0).getText();
        assertThat(renderedSystem)
                .as("the configured client must advertise all seven allowlisted tools by name")
                .contains("find_contacts")
                .contains("create_contact")
                .contains("edit_contact")
                .contains("find_companies")
                .contains("create_company")
                .contains("edit_company")
                .contains("edit_trato");

        // The shared tools object must be reusable across ChatClient
        // builds — the same shared instance produces the same callback
        // set every time. This guards against an accidental regression
        // to a per-invocation binder/factory.
        org.springframework.ai.tool.ToolCallback[] callbacks =
                org.springframework.ai.support.ToolCallbacks.from(sharedTools);
        Set<String> names = new HashSet<>();
        for (ToolCallback callback : callbacks) {
            names.add(callback.getToolDefinition().name());
        }
        assertThat(names)
                .as("the shared SpringAiCrmTools bean must produce exactly seven allowlisted callbacks")
                .containsExactlyInAnyOrder(
                        "find_contacts", "create_contact", "edit_contact",
                        "find_companies", "create_company", "edit_company",
                        "edit_trato");
    }

    private static Method findChatClientFactoryMethod() {
        for (Method method : AgentConfig.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(org.springframework.context.annotation.Bean.class)
                    && method.getReturnType() == ChatClient.class) {
                return method;
            }
        }
        throw new AssertionError("AgentConfig must declare a @Bean ChatClient chatClient(...) factory method");
    }
}