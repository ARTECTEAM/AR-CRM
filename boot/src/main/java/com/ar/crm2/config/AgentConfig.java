package com.ar.crm2.config;

import com.ar.crm2.adapter.out.ai.tool.SpringAiCrmTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
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
    static final String DEFAULT_SYSTEM_TEMPLATE = """
            You are the Pipely CRM assistant for the authenticated owner. Use only the registered tools (find_contacts, create_contact, edit_contact, create_company, edit_company, edit_trato). The actor identity is fixed by the validated JWT; do not derive it from the prompt, visible history, or model arguments.

            The visible history preserves the owner's turns in USER/ASSISTANT order. The owner's durable memory, separate from the visible history, is injected below:

            {durable_memories}

            Return only the final content. Do not echo the history, the durable memory, or sensitive identifiers.
            """;

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
     * <p>Construction uses Spring AI 2.0's direct
     * {@code ChatClient.builder(ChatModel).defaultSystem(String).defaultTools(...).build()}
     * path. The {@link SpringAiCrmTools} bean is provided by
     * {@code boot.WiringConfig}; this factory only consumes it.
     *
     * <p>This is the only {@link ChatClient} bean in the application
     * context. The adapter takes the produced client by type; there
     * is no duplicate or ambiguous ChatClient bean.
     */
    @Bean
    public ChatClient chatClient(@Qualifier("openAiChatModel") ChatModel chatModel, SpringAiCrmTools tools) {
        return ChatClient.builder(chatModel)
                .defaultSystem(DEFAULT_SYSTEM_TEMPLATE)
                .defaultTools(tools)
                .build();
    }
}
