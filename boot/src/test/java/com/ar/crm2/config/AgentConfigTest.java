package com.ar.crm2.config;

import com.ar.crm2.adapter.out.ai.tool.AgendaTools;
import com.ar.crm2.adapter.out.ai.tool.ColumnaTools;
import com.ar.crm2.adapter.out.ai.tool.ContactoTools;
import com.ar.crm2.adapter.out.ai.tool.EmpresaTools;
import com.ar.crm2.adapter.out.ai.tool.EtiquetaTools;
import com.ar.crm2.adapter.out.ai.tool.FichaTools;
import com.ar.crm2.adapter.out.ai.tool.TableroTools;
import com.ar.crm2.adapter.out.ai.tool.TareaTools;
import com.ar.crm2.adapter.out.ai.tool.TratoTools;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.EditContactoUseCase;
import com.ar.crm2.application.contacto.port.in.SearchContactosForActorUseCase;
import com.ar.crm2.application.empresa.port.in.CreateEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.EditEmpresaUseCase;
import com.ar.crm2.application.trato.port.in.EditTratoUseCase;
import com.ar.crm2.config.testing.CapturingChatModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Focused contract for the production
 * {@link AgentConfig} bean that owns the CRM agent's
 * {@link ChatClient} and its {@code defaultSystem} template.
 *
 * <p>AgentConfig composes nine resource-grouped tool beans and registers them together via
 * {@code ChatClient.Builder#defaultTools(Object...)}. The configured
 * {@link ChatClient} exposes all 50 allowlisted CRM tools to every
 * request; the per-request actor identity travels separately through
 * {@code ChatClient.RequestSpec#toolContext(...)} set by the adapter.
 */
class AgentConfigTest {

    @Test
    void configuredCallbacksConstrainAllDomainEnumsWithoutChangingOtherSchemaFields() throws Exception {
        var enums = new java.util.HashMap<>(Map.of(
                "estadoRelacion", com.ar.crm2.model.enums.EstadoRelacion.class,
                "tipoContrato", com.ar.crm2.model.enums.TipoContrato.class,
                "tipoTablero", com.ar.crm2.model.enums.TipoTablero.class,
                "tipoColumna", com.ar.crm2.model.enums.TipoColumna.class,
                "tipoFicha", com.ar.crm2.model.enums.TipoFicha.class));
        enums.put("tipo", com.ar.crm2.model.enums.TipoTarea.class);
        enums.put("prioridad", com.ar.crm2.model.enums.PrioridadTarea.class);
        enums.put("tipoEtiqueta", com.ar.crm2.model.enums.TipoEtiqueta.class);
        Object[] tools = newNoopTools();
        List<Class<?>> groups = List.of(TableroTools.class, ColumnaTools.class, FichaTools.class,
                ContactoTools.class, EmpresaTools.class, TratoTools.class, TareaTools.class,
                EtiquetaTools.class, AgendaTools.class);
        for (Class<?> group : groups) {
            for (var method : group.getDeclaredMethods()) {
                for (var parameter : method.getParameters()) {
                    if (enums.containsKey(parameter.getName())) {
                        Class<?> expected = group == AgendaTools.class && "tipo".equals(parameter.getName())
                                ? com.ar.crm2.model.enums.TipoAgenda.class : enums.get(parameter.getName());
                        assertThat(parameter.getType()).isEqualTo(expected);
                    }
                }
            }
        }
        CapturingChatModel model = new CapturingChatModel("ok");
        new AgentConfig().chatClient(model, tools).prompt().user("hi").call().content();
        var callbacks = ((ToolCallingChatOptions) model.capturedPrompt().getOptions()).getToolCallbacks();
        var originals = java.util.Arrays.stream(org.springframework.ai.support.ToolCallbacks.from(tools))
                .collect(java.util.stream.Collectors.toMap(c -> c.getToolDefinition().name(), c -> c));
        assertThat(callbacks).hasSize(50);
        assertThat(callbacks.stream().map(c -> c.getToolDefinition().name())).containsExactlyInAnyOrderElementsOf(originals.keySet());
        ObjectMapper mapper = new ObjectMapper();
        int constrained = 0;
        for (ToolCallback callback : callbacks) {
            var definition = callback.getToolDefinition();
            var original = originals.get(definition.name());
            var schema = mapper.readTree(definition.inputSchema());
            var unchanged = schema.deepCopy();
            assertThat(definition.description()).isEqualTo(original.getToolDefinition().description());
            assertThat(callback.getToolMetadata()).isEqualTo(original.getToolMetadata());
            for (var entry : enums.entrySet()) {
                var property = schema.path("properties").get(entry.getKey());
                if (property == null) continue;
                constrained++;
                Class<?> enumType = "tipo".equals(entry.getKey())
                        && java.util.Set.of("create_agenda", "edit_agenda").contains(definition.name())
                        ? com.ar.crm2.model.enums.TipoAgenda.class : entry.getValue();
                var names = java.util.Arrays.stream(enumType.getEnumConstants())
                        .map(value -> ((Enum<?>) value).name()).toList();
                assertThat(property.path("enum")).as("%s.%s", definition.name(), entry.getKey())
                        .isEqualTo(mapper.valueToTree(names));
                assertThat(property.path("type").asText()).isEqualTo("string");
            }
            assertThat(unchanged).isEqualTo(mapper.readTree(original.getToolDefinition().inputSchema()));
        }
        assertThat(constrained).isEqualTo(22);
    }

    private static Object[] newNoopTools() {
        return newTools(mock(SearchContactosForActorUseCase.class));
    }

    private static Object[] newTools(SearchContactosForActorUseCase getAllContactosUseCase) {
        return new Object[]{
                new TableroTools(mock(com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase.class),
                        mock(com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase.class),
                        mock(com.ar.crm2.application.tablero.port.in.CreateTableroUseCase.class),
                        mock(com.ar.crm2.application.tablero.port.in.EditTableroUseCase.class),
                        mock(com.ar.crm2.application.tablero.port.in.DeleteTableroUseCase.class),
                        mock(com.ar.crm2.application.tablero.port.in.EliminarColumnaDelTableroUseCase.class),
                        mock(com.ar.crm2.application.tablero.port.in.AsignarColumnaTableroUseCase.class),
                        mock(com.ar.crm2.application.tablero.port.in.ReordenarColumnasUseCase.class)),
                new ColumnaTools(mock(com.ar.crm2.application.columna.port.in.CreateColumnaUseCase.class),
                        mock(com.ar.crm2.application.columna.port.in.GetAllColumnasUseCase.class),
                        mock(com.ar.crm2.application.columna.port.in.GetColumnaByIdUseCase.class),
                        mock(com.ar.crm2.application.columna.port.in.EditColumnaUseCase.class),
                        mock(com.ar.crm2.application.columna.port.in.DeleteColumnaUseCase.class)),
                new FichaTools(mock(com.ar.crm2.application.ficha.port.in.CreateFichaUseCase.class),
                        mock(com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase.class),
                        mock(com.ar.crm2.application.ficha.port.in.GetFichaByIdUseCase.class),
                        mock(com.ar.crm2.application.ficha.port.in.EditFichaUseCase.class),
                        mock(com.ar.crm2.application.ficha.port.in.DeleteFichaUseCase.class),
                        mock(com.ar.crm2.application.ficha.port.in.MoverColumnaFichaUseCase.class)),
                new ContactoTools(getAllContactosUseCase, mock(CreateContactoUseCase.class),
                        mock(EditContactoUseCase.class), mock(com.ar.crm2.application.contacto.port.in.GetContactoByIdUseCase.class),
                        mock(com.ar.crm2.application.contacto.port.in.DeleteContactoUseCase.class),
                        mock(com.ar.crm2.application.contacto.port.in.CambiarEstadoContactoUseCase.class)),
                new EmpresaTools(mock(CreateEmpresaUseCase.class),
                        mock(com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase.class),
                        mock(EditEmpresaUseCase.class), mock(com.ar.crm2.application.empresa.port.in.DeleteEmpresaUseCase.class),
                        mock(com.ar.crm2.application.empresa.port.in.CambiarEstadoEmpresaUseCase.class)),
                new TratoTools(mock(com.ar.crm2.application.trato.port.in.CreateTratoUseCase.class),
                        mock(com.ar.crm2.application.trato.port.in.GetAllTratosUseCase.class),
                        mock(com.ar.crm2.application.trato.port.in.GetTratoByIdUseCase.class),
                        mock(EditTratoUseCase.class), mock(com.ar.crm2.application.trato.port.in.DeleteTratoUseCase.class)),
                new TareaTools(mock(com.ar.crm2.application.tarea.port.in.CreateTareaUseCase.class),
                        mock(com.ar.crm2.application.tarea.port.in.GetAllTareasUseCase.class),
                        mock(com.ar.crm2.application.tarea.port.in.GetTareaByIdUseCase.class),
                        mock(com.ar.crm2.application.tarea.port.in.EditTareaUseCase.class),
                        mock(com.ar.crm2.application.tarea.port.in.DeleteTareaUseCase.class)),
                new EtiquetaTools(mock(com.ar.crm2.application.etiqueta.port.in.CreateEtiquetaUseCase.class),
                        mock(com.ar.crm2.application.etiqueta.port.in.GetAllEtiquetasUseCase.class),
                        mock(com.ar.crm2.application.etiqueta.port.in.GetEtiquetaByIdUseCase.class),
                        mock(com.ar.crm2.application.etiqueta.port.in.EditEtiquetaUseCase.class),
                        mock(com.ar.crm2.application.etiqueta.port.in.DeleteEtiquetaUseCase.class)),
                new AgendaTools(mock(com.ar.crm2.application.agenda.port.in.CreateAgendaUseCase.class),
                        mock(com.ar.crm2.application.agenda.port.in.GetAgendasByUserUseCase.class),
                        mock(com.ar.crm2.application.agenda.port.in.GetAgendaByIdUseCase.class),
                        mock(com.ar.crm2.application.agenda.port.in.EditAgendaUseCase.class),
                        mock(com.ar.crm2.application.agenda.port.in.DeleteAgendaUseCase.class))};
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
    void chatClientFactoryConsumesAllResourceToolGroups() {
        Method chatClientMethod = findChatClientFactoryMethod();

        Parameter[] parameters = chatClientMethod.getParameters();
        Set<Class<?>> parameterTypes = java.util.Arrays.stream(parameters)
                .map(Parameter::getType).collect(java.util.stream.Collectors.toSet());
        assertThat(parameterTypes).contains(TableroTools.class, ColumnaTools.class, FichaTools.class,
                ContactoTools.class, EmpresaTools.class, TratoTools.class, TareaTools.class,
                EtiquetaTools.class, AgendaTools.class);
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
                .contains("create_company")
                .contains("edit_company", "edit_trato", "list_tableros", "get_tablero", "create_tablero",
                        "edit_tablero", "delete_tablero", "eliminar_columna_del_tablero",
                        "assign_columna_to_tablero", "reorder_tablero_columns", "list_columnas",
                        "get_columna", "create_columna", "edit_columna", "delete_columna",
                        "list_fichas", "get_ficha", "create_ficha", "edit_ficha", "delete_ficha",
                        "move_ficha_to_columna", "create_tarea", "list_tareas", "get_tarea",
                        "edit_tarea", "delete_tarea", "create_etiqueta", "list_etiquetas",
                        "get_etiqueta", "edit_etiqueta", "delete_etiqueta", "create_agenda",
                        "list_agendas", "get_agenda", "edit_agenda", "delete_agenda",
                        "get_contact", "change_contact_state", "delete_contact", "list_companies",
                        "change_company_state", "delete_company", "create_trato", "list_tratos",
                        "get_trato", "delete_trato");
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
                .as("destructive tools and label deletion confirmation policy are disclosed")
                .contains("delete_company", "Only delete records after the owner clearly requests deletion",
                        "pass confirm=true only after explicit confirmation");
        assertThat(text)
                .as("write choices belong to the owner, not silent model defaults")
                .contains("explicit choices in the current conversation", "scoped permission", "wait for the owner's reply",
                        "Permission already given", "disclose which values you chose", "never fabricate optional facts",
                        "Read-only queries do not need this clarification", "preserve unchanged fields",
                        "never silently clear", "available read tools", "ask when a match is ambiguous",
                        "name, description, and board type", "do not default to TAREAS");
    }

    @Test
    void boardCallbackExplainsMissingInputPolicyAndUserChosenType() throws Exception {
        CapturingChatModel model = new CapturingChatModel("ok");
        new AgentConfig().chatClient(model, newNoopTools()).prompt().user("hi").call().content();
        var callbacks = ((ToolCallingChatOptions) model.capturedPrompt().getOptions()).getToolCallbacks();
        var board = callbacks.stream().filter(c -> c.getToolDefinition().name().equals("create_tablero"))
                .findFirst().orElseThrow().getToolDefinition();
        assertThat(board.description()).contains("ask for missing description/type", "permission to choose them");
        var properties = new ObjectMapper().readTree(board.inputSchema()).path("properties");
        assertThat(properties.path("descripcion").path("description").asText()).contains("owner", "permission");
        assertThat(properties.path("tipoTablero").path("description").asText())
                .contains("owner", "permission", "Never silently default");
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
    void resourceToolGroupsAreRegisteredAsDefaultToolsOnTheConfiguredChatClient() {
         // The configured ChatClient must expose the full allowlisted catalog
        // through the maintained Spring AI 2.0 defaultTools path. The
        // exact tool names appear in the ChatClient's default callbacks
        // (introspected via getToolCallbacks() if exposed; otherwise via
        // the round-trip prompt that carries the schema to the model).
        CapturingChatModel model = new CapturingChatModel("ok");
        Object[] toolGroups = newNoopTools();
        ChatClient configured = new AgentConfig().chatClient(model, toolGroups);

        // Round-trip exercises the configured ChatClient end-to-end.
        // The captured prompt includes the tool definitions sent to the
         // model. All allowlisted tool names must be present.
        configured.prompt()
                .system(spec -> spec.param("durable_memories", ""))
                .user("hi")
                .call()
                .content();

        String renderedSystem = model.capturedPrompt().getInstructions().get(0).getText();
        assertThat(renderedSystem)
                .as("the configured client must advertise all allowlisted tools by name")
                .contains("find_contacts")
                .contains("create_contact")
                .contains("edit_contact")
                .contains("create_company")
                .contains("edit_company", "edit_trato", "list_tableros", "create_columna", "edit_ficha",
                        "delete_company", "create_agenda", "delete_tarea", "delete_ficha");

        // The stateless resource groups are reusable across ChatClient builds.
        org.springframework.ai.tool.ToolCallback[] callbacks =
                org.springframework.ai.support.ToolCallbacks.from(toolGroups);
        Set<String> names = new HashSet<>();
        for (ToolCallback callback : callbacks) {
            names.add(callback.getToolDefinition().name());
        }
        assertThat(names)
                 .as("the resource tool groups must produce exactly the allowlisted callbacks")
                 .containsExactlyInAnyOrder(
                         "find_contacts", "create_contact", "get_contact", "edit_contact", "change_contact_state", "delete_contact",
                         "create_company", "list_companies", "edit_company", "change_company_state", "delete_company",
                         "create_trato", "list_tratos", "get_trato", "edit_trato", "delete_trato",
                         "create_tarea", "list_tareas", "get_tarea", "edit_tarea", "delete_tarea",
                         "create_etiqueta", "list_etiquetas", "get_etiqueta", "edit_etiqueta", "delete_etiqueta",
                         "create_agenda", "list_agendas", "get_agenda", "edit_agenda", "delete_agenda",
                         "list_tableros", "get_tablero", "create_tablero", "edit_tablero", "delete_tablero",
                         "eliminar_columna_del_tablero", "assign_columna_to_tablero", "reorder_tablero_columns",
                         "list_columnas", "get_columna", "create_columna", "edit_columna", "delete_columna",
                         "list_fichas", "get_ficha", "create_ficha", "edit_ficha", "delete_ficha", "move_ficha_to_columna");
    }

    @Test
    void realToolLoopRedactsSensitiveDownstreamFailureBeforeSecondModelRequest() {
        String sentinel = "SENSITIVE_SQL_PROVIDER_DETAIL";
        SearchContactosForActorUseCase useCase = mock(SearchContactosForActorUseCase.class);
        when(useCase.search(any())).thenThrow(new IllegalStateException(sentinel));
        SequentialToolCallingChatModel model =
                new SequentialToolCallingChatModel("find_contacts", "{}");
        ChatClient configured = new AgentConfig().buildChatClient(
                model, new com.ar.crm2.adapter.out.ai.tool.SafeToolExecutionExceptionProcessor(),
                newTools(useCase));

        String content = configured.prompt()
                .system(spec -> spec.param("durable_memories", ""))
                .user("find contacts")
                .toolContext(Map.of("actorUsuarioId", UUID.randomUUID()))
                .call()
                .content();

        assertThat(content).isEqualTo("final-response");
        assertThat(model.prompts()).hasSize(2);
        String result = toolResponseData(model.prompts().get(1));
        assertThat(result)
                .isEqualTo("{\"success\":false,\"code\":\"TOOL_EXECUTION_FAILED\","
                        + "\"message\":\"The tool could not be completed.\"}")
                .doesNotContain(sentinel)
                .doesNotContain("IllegalStateException");
        assertThat(model.prompts().get(1).getContents()).doesNotContain(sentinel);
    }

    @Test
    void realToolLoopReturnsOnlyStableCodeForExplicitSafeValidationFailure() {
        SequentialToolCallingChatModel model = new SequentialToolCallingChatModel(
                "create_contact", "{\"nombre\":\"Ada\",\"estadoRelacion\":\"ACTIVO\"}");
        ChatClient configured = new AgentConfig().buildChatClient(
                model, new com.ar.crm2.adapter.out.ai.tool.SafeToolExecutionExceptionProcessor(),
                newNoopTools());

        configured.prompt()
                .system(spec -> spec.param("durable_memories", ""))
                .user("create contact")
                .toolContext(Map.of("actorUsuarioId", UUID.randomUUID()))
                .call()
                .content();

        assertThat(model.prompts()).hasSize(2);
        assertThat(toolResponseData(model.prompts().get(1)))
                .isEqualTo("{\"success\":false,\"code\":\"TOOL_VALIDATION_FAILED\","
                        + "\"message\":\"The tool input is invalid.\"}")
                .doesNotContain("empresaId");
    }

    private static String toolResponseData(Prompt prompt) {
        ToolResponseMessage message = prompt.getInstructions().stream()
                .filter(ToolResponseMessage.class::isInstance)
                .map(ToolResponseMessage.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(message.getResponses()).hasSize(1);
        return message.getResponses().get(0).responseData();
    }

    private static final class SequentialToolCallingChatModel implements ChatModel {
        private final String toolName;
        private final String arguments;
        private final List<Prompt> prompts = new ArrayList<>();

        private SequentialToolCallingChatModel(String toolName, String arguments) {
            this.toolName = toolName;
            this.arguments = arguments;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            if (prompts.size() == 1) {
                AssistantMessage toolCall = AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "call-1", "function", toolName, arguments)))
                        .build();
                return new ChatResponse(List.of(new Generation(toolCall)));
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("final-response"))));
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        private List<Prompt> prompts() {
            return List.copyOf(prompts);
        }
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
