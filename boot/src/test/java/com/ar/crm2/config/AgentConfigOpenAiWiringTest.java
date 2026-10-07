package com.ar.crm2.config;

import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.EditContactoUseCase;
import com.ar.crm2.application.contacto.port.in.SearchContactosForActorUseCase;
import com.ar.crm2.application.empresa.port.in.CreateEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.EditEmpresaUseCase;
import com.ar.crm2.application.trato.port.in.EditTratoUseCase;
import com.ar.crm2.adapter.out.ai.tool.AgendaTools;
import com.ar.crm2.adapter.out.ai.tool.ColumnaTools;
import com.ar.crm2.adapter.out.ai.tool.ContactoTools;
import com.ar.crm2.adapter.out.ai.tool.EmpresaTools;
import com.ar.crm2.adapter.out.ai.tool.EtiquetaTools;
import com.ar.crm2.adapter.out.ai.tool.FichaTools;
import com.ar.crm2.adapter.out.ai.tool.TableroTools;
import com.ar.crm2.adapter.out.ai.tool.TareaTools;
import com.ar.crm2.adapter.out.ai.tool.TratoTools;
import com.ar.crm2.config.testing.CapturingChatModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Narrow no-network Spring context wiring proof for the production
 * {@link AgentConfig} bean.
 *
 * <p>Wires the production {@code AgentConfig} together with a
 * deterministic {@code @Bean ChatModel openAiChatModel()} — naming
 * the bean exactly the way
 * {@code OpenAiChatAutoConfiguration#openAiChatModel} would — and a
 * deterministic resource-group tool beans (no per-invocation binder). The configured
 * {@link ChatClient} is round-tripped through the real Spring AI
 * request path with the owned production defaultSystem template.
 *
 * <p>No network, no credentials, and no full Spring Boot context are
 * involved: the test uses a focused
 * {@link AnnotationConfigApplicationContext} with {@link Import} on the
 * production {@link AgentConfig}.
 */
class AgentConfigOpenAiWiringTest {

    private static com.ar.crm2.adapter.out.ai.tool.CrmToolOutputProjector outputProjector() {
        var authorization = mock(com.ar.crm2.application.security.CrmAuthorization.class);
        var groups = java.util.Set.of(com.ar.crm2.model.autorizacion.GrupoCampoSensible.values());
        when(authorization.fieldPolicy(org.mockito.ArgumentMatchers.any())).thenReturn(
                new com.ar.crm2.application.security.ResourceReadPolicy(
                        com.ar.crm2.model.autorizacion.AlcanceCrm.TODO_COMPARTIDO,
                        groups, java.util.Set.of(), groups));
        return new com.ar.crm2.adapter.out.ai.tool.CrmToolOutputProjector(authorization);
    }
    @Configuration
    @Import(AgentConfig.class)
    static class OpenAiTestContext {

        @Bean
        ChatModel openAiChatModel() {
            return new CapturingChatModel("ok-from-context");
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean TableroTools tableroTools() {
            return new TableroTools(mock(com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase.class),
                    mock(com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase.class),
                    mock(com.ar.crm2.application.tablero.port.in.CreateTableroUseCase.class),
                    mock(com.ar.crm2.application.tablero.port.in.EditTableroUseCase.class),
                    mock(com.ar.crm2.application.tablero.port.in.DeleteTableroUseCase.class),
                    mock(com.ar.crm2.application.tablero.port.in.EliminarColumnaDelTableroUseCase.class),
                    mock(com.ar.crm2.application.tablero.port.in.AsignarColumnaTableroUseCase.class),
                    mock(com.ar.crm2.application.tablero.port.in.ReordenarColumnasUseCase.class), outputProjector());
        }
        @Bean ColumnaTools columnaTools() {
            return new ColumnaTools(mock(com.ar.crm2.application.columna.port.in.CreateColumnaUseCase.class),
                    mock(com.ar.crm2.application.columna.port.in.GetAllColumnasUseCase.class),
                    mock(com.ar.crm2.application.columna.port.in.GetColumnaByIdUseCase.class),
                    mock(com.ar.crm2.application.columna.port.in.EditColumnaUseCase.class),
                    mock(com.ar.crm2.application.columna.port.in.DeleteColumnaUseCase.class));
        }
        @Bean FichaTools fichaTools() {
            return new FichaTools(mock(com.ar.crm2.application.ficha.port.in.CreateFichaUseCase.class),
                    mock(com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase.class),
                    mock(com.ar.crm2.application.ficha.port.in.GetFichaByIdUseCase.class),
                    mock(com.ar.crm2.application.ficha.port.in.EditFichaUseCase.class),
                    mock(com.ar.crm2.application.ficha.port.in.DeleteFichaUseCase.class),
                    mock(com.ar.crm2.application.ficha.port.in.MoverColumnaFichaUseCase.class));
        }
        @Bean ContactoTools contactoTools() {
            return new ContactoTools(mock(SearchContactosForActorUseCase.class), mock(CreateContactoUseCase.class),
                    mock(EditContactoUseCase.class), mock(com.ar.crm2.application.contacto.port.in.GetContactoByIdUseCase.class),
                    mock(com.ar.crm2.application.contacto.port.in.DeleteContactoUseCase.class),
                    mock(com.ar.crm2.application.contacto.port.in.CambiarEstadoContactoUseCase.class), outputProjector());
        }
        @Bean EmpresaTools empresaTools() {
            return new EmpresaTools(mock(CreateEmpresaUseCase.class),
                    mock(com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase.class),
                    mock(EditEmpresaUseCase.class), mock(com.ar.crm2.application.empresa.port.in.DeleteEmpresaUseCase.class),
                    mock(com.ar.crm2.application.empresa.port.in.CambiarEstadoEmpresaUseCase.class), outputProjector());
        }
        @Bean TratoTools tratoTools() {
            return new TratoTools(mock(com.ar.crm2.application.trato.port.in.CreateTratoUseCase.class),
                    mock(com.ar.crm2.application.trato.port.in.GetAllTratosUseCase.class),
                    mock(com.ar.crm2.application.trato.port.in.GetTratoByIdUseCase.class),
                    mock(EditTratoUseCase.class), mock(com.ar.crm2.application.trato.port.in.DeleteTratoUseCase.class), outputProjector());
        }
        @Bean TareaTools tareaTools() {
            return new TareaTools(mock(com.ar.crm2.application.tarea.port.in.CreateTareaUseCase.class),
                    mock(com.ar.crm2.application.tarea.port.in.GetAllTareasUseCase.class),
                    mock(com.ar.crm2.application.tarea.port.in.GetTareaByIdUseCase.class),
                    mock(com.ar.crm2.application.tarea.port.in.EditTareaUseCase.class),
                    mock(com.ar.crm2.application.tarea.port.in.DeleteTareaUseCase.class));
        }
        @Bean EtiquetaTools etiquetaTools() {
            return new EtiquetaTools(mock(com.ar.crm2.application.etiqueta.port.in.CreateEtiquetaUseCase.class),
                    mock(com.ar.crm2.application.etiqueta.port.in.GetAllEtiquetasUseCase.class),
                    mock(com.ar.crm2.application.etiqueta.port.in.GetEtiquetaByIdUseCase.class),
                    mock(com.ar.crm2.application.etiqueta.port.in.EditEtiquetaUseCase.class),
                    mock(com.ar.crm2.application.etiqueta.port.in.DeleteEtiquetaUseCase.class));
        }
        @Bean AgendaTools agendaTools() {
            return new AgendaTools(mock(com.ar.crm2.application.agenda.port.in.CreateAgendaUseCase.class),
                    mock(com.ar.crm2.application.agenda.port.in.GetAgendasByUserUseCase.class),
                    mock(com.ar.crm2.application.agenda.port.in.GetAgendaByIdUseCase.class),
                    mock(com.ar.crm2.application.agenda.port.in.EditAgendaUseCase.class),
                    mock(com.ar.crm2.application.agenda.port.in.DeleteAgendaUseCase.class));
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
                    .system(spec -> spec.param("durable_memories", "- wired-memory").param("agent_capabilities", "No CRM tool capabilities are available for this turn."))
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
                    .system(spec -> spec.param("durable_memories", "- wired-memory-2").param("agent_capabilities", "No CRM tool capabilities are available for this turn."))
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
    void groupedResourceToolsAreResolvedAsTheExactCatalogWithoutGlobalDefaults() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                OpenAiTestContext.class)) {

            assertThat(context.getBean(TableroTools.class)).isNotNull();
            assertThat(context.getBean(ColumnaTools.class)).isNotNull();
            assertThat(context.getBean(FichaTools.class)).isNotNull();
            assertThat(context.getBean(ContactoTools.class)).isNotNull();
            assertThat(context.getBean(EmpresaTools.class)).isNotNull();
            assertThat(context.getBean(TratoTools.class)).isNotNull();
            assertThat(context.getBean(TareaTools.class)).isNotNull();
            assertThat(context.getBean(EtiquetaTools.class)).isNotNull();
            assertThat(context.getBean(AgendaTools.class)).isNotNull();

            var catalog = context.getBean(com.ar.crm2.adapter.out.ai.tool.AgentToolCallbackCatalog.class);
            assertThat(catalog.registeredToolNames()).hasSize(50)
                    .contains("find_contacts", "create_contact", "list_tableros", "move_ficha_to_columna");

            ChatClient chatClient = context.getBean(ChatClient.class);
            chatClient.prompt()
                    .system(spec -> spec.param("durable_memories", "").param("agent_capabilities", "No CRM tool capabilities are available for this turn."))
                    .user("hi")
                    .call()
                    .content();

            Object promptOptions = ((CapturingChatModel) context.getBean("openAiChatModel"))
                    .capturedPrompt().getOptions();
            if (promptOptions instanceof ToolCallingChatOptions options) {
                assertThat(options.getToolCallbacks()).isNullOrEmpty();
            }
        }
    }
    @Test
    void wiredContextClosesCleanlyAfterOpenAiChatClientRoundTrip() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                OpenAiTestContext.class)) {
            ChatClient chatClient = context.getBean(ChatClient.class);
            chatClient.prompt()
                    .system(spec -> spec.param("durable_memories", "- x").param("agent_capabilities", "No CRM tool capabilities are available for this turn."))
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
