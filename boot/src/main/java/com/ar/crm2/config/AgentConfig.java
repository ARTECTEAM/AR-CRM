package com.ar.crm2.config;

import com.ar.crm2.adapter.out.ai.tool.AgendaTools;
import com.ar.crm2.adapter.out.ai.tool.ColumnaTools;
import com.ar.crm2.adapter.out.ai.tool.ContactoTools;
import com.ar.crm2.adapter.out.ai.tool.EmpresaTools;
import com.ar.crm2.adapter.out.ai.tool.EtiquetaTools;
import com.ar.crm2.adapter.out.ai.tool.FichaTools;
import com.ar.crm2.adapter.out.ai.tool.SafeToolExecutionExceptionProcessor;
import com.ar.crm2.adapter.out.ai.tool.TableroTools;
import com.ar.crm2.adapter.out.ai.tool.TareaTools;
import com.ar.crm2.adapter.out.ai.tool.TratoTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.observation.DefaultAdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.DefaultChatClientObservationConvention;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import io.micrometer.observation.ObservationRegistry;
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
 *       {@code {durable_memories}} placeholder, the 50 resource-grouped CRM
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
     *       50 allowlisted tools grouped by the nine CRM resources.</li>
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
    static final String TOOL_CATALOG =
            "find_contacts, create_contact, get_contact, edit_contact, change_contact_state, delete_contact"
            + ", create_company, list_companies, edit_company, change_company_state, delete_company"
            + ", create_trato, list_tratos, get_trato, edit_trato, delete_trato"
            + ", create_tarea, list_tareas, get_tarea, edit_tarea, delete_tarea"
            + ", create_etiqueta, list_etiquetas, get_etiqueta, edit_etiqueta, delete_etiqueta"
            + ", create_agenda, list_agendas, get_agenda, edit_agenda, delete_agenda"
            + ", list_tableros, get_tablero, create_tablero, edit_tablero, delete_tablero,"
            + " eliminar_columna_del_tablero, assign_columna_to_tablero, reorder_tablero_columns"
            + ", list_columnas, get_columna, create_columna, edit_columna, delete_columna"
            + ", list_fichas, get_ficha, create_ficha, edit_ficha, delete_ficha, move_ficha_to_columna";

    static final String DEFAULT_SYSTEM_TEMPLATE = systemTemplate(TOOL_CATALOG);

    private static String systemTemplate(String catalog) {
        return """
            You are the Pipely CRM assistant for the authenticated owner. Use only the registered tools (%s). The actor identity is fixed by the validated JWT; do not derive it from the prompt, visible history, or model arguments.

            Before a write tool, use the owner's explicit choices in the current conversation for important business inputs (names, descriptions, types, relationship states, assignments, amounts, dates, and ordering). A required tool parameter is not permission to invent its value.
            If an important choice is missing, explain the missing inputs and ask for them or for scoped permission to choose them; wait for the owner's reply before writing. Permission already given in the current request or conversation covers only its stated scope; do not ask again for choices it covers. After an authorized write, disclose which values you chose. Use only defaults explicitly documented by the tool or domain; never fabricate optional facts such as contact details. Omit unspecified optional inputs only when the tool supports omission without unintended changes. Do not turn every optional field into a mandatory questionnaire.
            Only delete records after the owner clearly requests deletion. For delete_etiqueta, pass confirm=true only after explicit confirmation; otherwise omit it or pass false.
            For create_tablero, prefer the owner to provide name, description, and board type. For a name-only request, ask for missing description and type (TAREAS or TRATOS), or permission to choose them; do not default to TAREAS. Permission to choose allows you to supply missing values, not to bypass required tool or domain inputs.
            Resolve existing entity references using available read tools and ask when a match is ambiguous; never invent UUIDs. If the required lookup is unavailable, ask the owner for the missing reference. Read-only queries do not need this clarification unless their target is ambiguous.
            For edits, preserve unchanged fields using available reads or values explicitly provided in the conversation. If you cannot recover a required existing value, ask rather than guess. For full replacements (labels or column order), establish the intended complete set; never silently clear or drop unspecified entries.

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
     * registers the nine stateless resource tool beans once via
     * {@code defaultTools(...)}. The resulting {@link ChatClient} advertises all 50 allowlisted
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
     * The resource tool beans are provided by {@code boot.WiringConfig}; this factory only consumes them.
     *
     * <p>This is the only {@link ChatClient} bean in the application
     * context. The adapter takes the produced client by type; there
     * is no duplicate or ambiguous ChatClient bean.
     */
    @Bean
    public ChatClient chatClient(
            @Qualifier("openAiChatModel") ChatModel chatModel,
            TableroTools tableroTools,
            ColumnaTools columnaTools,
            FichaTools fichaTools,
            ContactoTools contactoTools,
            EmpresaTools empresaTools,
            TratoTools tratoTools,
            TareaTools tareaTools,
            EtiquetaTools etiquetaTools,
            AgendaTools agendaTools,
            ToolExecutionExceptionProcessor exceptionProcessor) {
        return buildChatClient(chatModel, exceptionProcessor, tableroTools, columnaTools, fichaTools,
                contactoTools, empresaTools, tratoTools, tareaTools, etiquetaTools, agendaTools);
    }

    ChatClient chatClient(ChatModel chatModel, Object... toolGroups) {
                return buildChatClient(chatModel, new SafeToolExecutionExceptionProcessor(), toolGroups);
    }

    ChatClient buildChatClient(ChatModel chatModel,
            ToolExecutionExceptionProcessor exceptionProcessor, Object... toolGroups) {
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
                .defaultSystem(systemTemplate(TOOL_CATALOG));
        return builder.defaultTools(toolGroups).build();
    }

    @Bean
    public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        return new SafeToolExecutionExceptionProcessor();
    }
}
