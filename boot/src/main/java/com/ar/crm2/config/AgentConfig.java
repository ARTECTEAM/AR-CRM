package com.ar.crm2.config;

import com.ar.crm2.adapter.out.ai.tool.SpringAiCrmTools;
import com.ar.crm2.adapter.out.ai.tool.SpringAiDevelopmentCrmTools;
import com.ar.crm2.adapter.out.ai.tool.SafeToolExecutionExceptionProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.observation.DefaultAdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.DefaultChatClientObservationConvention;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pipely CRM agent composition.
 *
 * <p>Owns the configured Spring AI 2.0 {@link ChatClient} for the CRM
 * agent and the {@code defaultSystem} template that the
 * {@link com.ar.crm2.adapter.out.ai.SpringAiChatCompletionAdapter} fills
 * at request time via
 * {@code ChatClient#prompt().system(Consumer<PromptSystemSpec>)}.
 *
 * <p>The injected {@link ChatModel} is the OpenAI provider starter bean
 * resolved through Spring AI 2.0's
 * {@code OpenAiChatAutoConfiguration#openAiChatModel} — brought in by
 * {@code org.springframework.ai:spring-ai-starter-model-openai} and
 * selected here by an explicit
 * {@link Qualifier @Qualifier("openAiChatModel")}. The qualifier is the
 * canonical bean name from the autoconfig's {@code @Bean} method and is
 * required to disambiguate the chosen provider at runtime.
 *
 * <p>Configuration is environment-backed via the standard Spring AI
 * properties — chiefly {@code spring.ai.openai.api-key} (env var
 * {@code OPENAI_API_KEY}); see {@code application.yml}. No secret is
 * hardcoded and no model is selected beyond the autoconfig default, so
 * the credential, base URL, and model choice stay operator-driven.
 *
 * <p>Why this lives in {@code boot} (and not in {@code infrastructure}):
 * <ul>
 *   <li>{@code Boot} is the composition root. The project rule
 *       "Boot only composes" applies: this class is a {@code @Configuration}
 *       with a {@code @Bean} that produces the configured client.</li>
 *   <li>The adapter-under-test in {@code infrastructure} cannot reach
 *       this class directly (the dependency direction is
 *       {@code boot -> infrastructure -> application -> domain}). The
 *       adapter only consumes the contract: a {@link ChatClient} with
 *       a {@code defaultSystem} template that contains the
 *       {@code {durable_memories}} placeholder, the six shared CRM
 *       tools registered once as {@code defaultTools}, and the
 *       {@code defaultToolCallbacks} Spring AI 2.0 introspects to
 *       generate the allowlist schemas.</li>
 *   <li>The system template is product-owned, not adapter-owned. It
 *       belongs next to the wiring (boot) and not next to the
 *       provider-neutral adapter (infrastructure).</li>
 * </ul>
 */
@Configuration
public class AgentConfig {

    /**
     * Production defaultSystem template for the Pipely CRM agent.
     *
     * <p>Scoped to existing Pipely CRM behavior — no invented product
     * rules. The template:
     * <ul>
     *   <li>States the agent identity (Pipely CRM assistant) and the
     *       six allowlisted tools ({@code find_contacts},
     *       {@code create_contact}, {@code edit_contact},
     *       {@code create_company}, {@code edit_company},
     *       {@code edit_trato}). Company deletion is intentionally
      *       NOT exposed and company search is outside the six-tool
      *       allowlist.</li>
     *   <li>Reiterates the identity discipline: the actor identity is
     *       fixed by the validated JWT and MUST NOT be derived from
     *       the prompt, the visible history, or the model arguments.</li>
     *   <li>References the visible history structure (USER/ASSISTANT
     *       ordering) and the durable memory placeholder, which is
     *       supplied at request time by the adapter.</li>
     *   <li>Instructs the model to return only the final content and
     *       not to echo the history, the durable memory, or sensitive
     *       identifiers.</li>
     * </ul>
     *
     * <p>The {@code {durable_memories}} placeholder is the only
     * parameter the adapter supplies at request time. There are no
     * other placeholders in the template; any value present in the
     * rendered prompt is either static or comes from the adapter's
     * {@code system(Consumer<PromptSystemSpec>)} call.
     */
    static final String DEFAULT_TOOL_CATALOG =
            "find_contacts, create_contact, edit_contact, create_company, edit_company, edit_trato";
    static final String DEVELOPMENT_TOOL_CATALOG = DEFAULT_TOOL_CATALOG
            + ", list_tableros, get_tablero, create_tablero, edit_tablero, assign_columna_to_tablero,"
            + " reorder_tablero_columns, list_columnas, get_columna, create_columna, edit_columna,"
            + " list_fichas, get_ficha, create_ficha, edit_ficha, move_ficha_to_columna";

    static final String DEFAULT_SYSTEM_TEMPLATE = systemTemplate(DEFAULT_TOOL_CATALOG);

    private static String systemTemplate(String catalog) {
        return """
            You are the Pipely CRM assistant for the authenticated owner. Use only the registered tools (%s). The actor identity is fixed by the validated JWT; do not derive it from the prompt, visible history, or model arguments.

            The visible history preserves the owner's turns in USER/ASSISTANT order. The owner's durable memory, separate from the visible history, is injected below:

            {durable_memories}

            Return only the final content. Do not echo the history, the durable memory, or sensitive identifiers.
            """.formatted(catalog);
    }

    /**
     * Configured {@link ChatClient} bean for the Pipely CRM agent.
     *
     * <p>Consumes the OpenAI provider starter {@link ChatModel} bean
     * (resolved by {@link Qualifier @Qualifier("openAiChatModel")}
     * from {@code OpenAiChatAutoConfiguration#openAiChatModel}),
     * applies the owned {@link #DEFAULT_SYSTEM_TEMPLATE}, and
     * registers the shared stateless {@link SpringAiCrmTools} bean
     * once via {@code ChatClient.Builder#defaultTools(Object...)}. The
     * resulting {@link ChatClient} advertises the six allowlisted
     * CRM tools to every request; the trusted CRM {@code actorUsuarioId}
     * travels separately per request via
     * {@code ChatClient.RequestSpec#toolContext(...)} set by the
     * adapter. The adapter does NOT call request {@code .tools(...)}
     * — Spring AI 2.0 runtime tools would replace builder defaults,
     * so omitting that call preserves the configured allowlist.
     *
     * <p>Construction supplies Spring AI 2.0's auto-registered
     * {@link ToolCallingAdvisor} builder with one explicit
     * {@link ToolCallingManager}. That manager owns the complete tool loop and
     * uses the configured {@link ToolExecutionExceptionProcessor}; no second
     * tool advisor is added. The advisor keeps its framework default order, so
     * any memory advisor remains outside the tool loop as Spring AI specifies.
     * The {@link SpringAiCrmTools} bean is provided by
     * {@code boot.WiringConfig}; this factory only consumes it.
     *
     * <p>This is the only {@link ChatClient} bean in the application
     * context. The adapter takes the produced client by type; there
     * is no duplicate or ambiguous ChatClient bean.
     */
    @Bean
    public ChatClient chatClient(
            @Qualifier("openAiChatModel") ChatModel chatModel,
            SpringAiCrmTools tools,
            ObjectProvider<SpringAiDevelopmentCrmTools> developmentToolsProvider,
            ToolExecutionExceptionProcessor exceptionProcessor) {
        return buildChatClient(
                chatModel, tools, developmentToolsProvider.getIfAvailable(), exceptionProcessor);
    }

    ChatClient chatClient(ChatModel chatModel, SpringAiCrmTools tools) {
        ObjectMapper objectMapper = new ObjectMapper();
        return buildChatClient(
                chatModel, tools, null, new SafeToolExecutionExceptionProcessor(objectMapper));
    }

    ChatClient buildChatClient(
            ChatModel chatModel,
            SpringAiCrmTools tools,
            SpringAiDevelopmentCrmTools developmentTools,
            ToolExecutionExceptionProcessor exceptionProcessor) {
        ToolCallingManager toolCallingManager = ToolCallingManager.builder()
                .observationRegistry(ObservationRegistry.NOOP)
                .toolExecutionExceptionProcessor(exceptionProcessor)
                .build();
        ToolCallingAdvisor.Builder<?> toolCallingAdvisor = ToolCallingAdvisor.builder()
                .toolCallingManager(toolCallingManager);
        ChatClient.Builder builder = ChatClient.builder(
                        chatModel,
                        ObservationRegistry.NOOP,
                        new DefaultChatClientObservationConvention(),
                        new DefaultAdvisorObservationConvention(),
                        toolCallingAdvisor)
                .defaultSystem(systemTemplate(developmentTools == null
                        ? DEFAULT_TOOL_CATALOG : DEVELOPMENT_TOOL_CATALOG));
        return developmentTools == null
                ? builder.defaultTools(tools).build()
                : builder.defaultTools(tools, developmentTools).build();
    }

    @Bean
    public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor(ObjectMapper objectMapper) {
        return new SafeToolExecutionExceptionProcessor(objectMapper);
    }
}
