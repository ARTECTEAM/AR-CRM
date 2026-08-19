package com.ar.crm2.config;

import com.ar.crm2.adapter.out.ai.tool.SpringAiDevelopmentCrmTools;
import com.ar.crm2.adapter.out.ai.tool.SpringAiCrmTools;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.EditContactoUseCase;
import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.application.empresa.port.in.CreateEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.EditEmpresaUseCase;
import com.ar.crm2.application.trato.port.in.EditTratoUseCase;
import com.ar.crm2.application.columna.port.in.CreateColumnaUseCase;
import com.ar.crm2.application.columna.port.in.EditColumnaUseCase;
import com.ar.crm2.application.columna.port.in.GetAllColumnasUseCase;
import com.ar.crm2.application.columna.port.in.GetColumnaByIdUseCase;
import com.ar.crm2.application.ficha.port.in.CreateFichaUseCase;
import com.ar.crm2.application.ficha.port.in.EditFichaUseCase;
import com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase;
import com.ar.crm2.application.ficha.port.in.GetFichaByIdUseCase;
import com.ar.crm2.application.ficha.port.in.MoverColumnaFichaUseCase;
import com.ar.crm2.application.tablero.port.in.AsignarColumnaTableroUseCase;
import com.ar.crm2.application.tablero.port.in.CreateTableroUseCase;
import com.ar.crm2.application.tablero.port.in.EditTableroUseCase;
import com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase;
import com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase;
import com.ar.crm2.application.tablero.port.in.ReordenarColumnasUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AgentDevelopmentToolsConfigTest {

    private static final Set<String> DEFAULT_CATALOG = Set.of(
            "find_contacts", "create_contact", "edit_contact",
            "create_company", "edit_company", "edit_trato");
    private static final Set<String> DEVELOPMENT_CATALOG = Set.of(
            "find_contacts", "create_contact", "edit_contact",
            "create_company", "edit_company", "edit_trato",
            "list_tableros", "get_tablero", "create_tablero", "edit_tablero",
            "assign_columna_to_tablero", "reorder_tablero_columns", "list_columnas",
            "get_columna", "create_columna", "edit_columna", "list_fichas", "get_ficha",
            "create_ficha", "edit_ficha", "move_ficha_to_columna");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Dependencies.class, AgentDevelopmentToolsConfig.class, AgentConfig.class);

    @Test
    void developmentToolsAreAbsentByDefault() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(SpringAiDevelopmentCrmTools.class);
            assertConfiguredClientCatalog(context.getBean(ChatClient.class),
                    context.getBean(com.ar.crm2.config.testing.CapturingChatModel.class),
                    DEFAULT_CATALOG, 6);
        });
    }

    @Test
    void developmentToolsAreAbsentWhenFlagIsExplicitlyFalse() {
        runner.withPropertyValues(
                "spring.profiles.active=prod",
                "crm2.agent.development-tools-enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(SpringAiDevelopmentCrmTools.class);
            assertConfiguredClientCatalog(context.getBean(ChatClient.class),
                    context.getBean(com.ar.crm2.config.testing.CapturingChatModel.class),
                    DEFAULT_CATALOG, 6);
        });
    }

    @Test
    void explicitFlagRegistersExactlyTwentyOneCallbacksInNoauthProfile() {
        runner.withPropertyValues(
                "spring.profiles.active=noauth",
                "crm2.agent.development-tools-enabled=true").run(context -> {
            assertThat(context).hasSingleBean(SpringAiDevelopmentCrmTools.class);
            assertConfiguredClientCatalog(context.getBean(ChatClient.class),
                    context.getBean(com.ar.crm2.config.testing.CapturingChatModel.class),
                    DEVELOPMENT_CATALOG, 21);
        });
    }

    @Test
    void explicitFlagRegistersExactlyTwentyOneCallbacksInTestProfile() {
        runner.withPropertyValues(
                "spring.profiles.active=test",
                "crm2.agent.development-tools-enabled=true").run(context -> {
            assertThat(context).hasSingleBean(SpringAiDevelopmentCrmTools.class);
            assertConfiguredClientCatalog(context.getBean(ChatClient.class),
                    context.getBean(com.ar.crm2.config.testing.CapturingChatModel.class),
                    DEVELOPMENT_CATALOG, 21);
        });
    }

    @Test
    void explicitFlagFailsStartupWithoutAnActiveProfile() {
        assertRejected("crm2.agent.development-tools-enabled=true");
    }

    @Test
    void explicitFlagFailsStartupForUnknownProfile() {
        assertRejected("spring.profiles.active=staging", "crm2.agent.development-tools-enabled=true");
    }

    @Test
    void explicitFlagFailsStartupWhenNoauthAndTestAreBothActive() {
        assertRejected("spring.profiles.active=noauth,test", "crm2.agent.development-tools-enabled=true");
    }

    @Test
    void explicitFlagFailsStartupWhenAcceptedAndProductionProfilesAreMixed() {
        assertRejected("spring.profiles.active=noauth,prod", "crm2.agent.development-tools-enabled=true");
        assertRejected("spring.profiles.active=test,production", "crm2.agent.development-tools-enabled=true");
    }

    @Test
    void explicitFlagFailsStartupInProductionProfile() {
        assertRejected("spring.profiles.active=prod", "crm2.agent.development-tools-enabled=true");
    }

    private void assertRejected(String... properties) {
        runner.withPropertyValues(properties).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage(AgentDevelopmentToolsEnvironmentGuard.CONFIGURATION_ERROR);
        });
    }

    private static void assertConfiguredClientCatalog(
            ChatClient chatClient,
            com.ar.crm2.config.testing.CapturingChatModel model,
            Set<String> expected,
            int expectedSize) {
        chatClient.prompt()
                .system(spec -> spec.param("durable_memories", ""))
                .user("catalog check")
                .call()
                .content();

        ToolCallingChatOptions options = (ToolCallingChatOptions) model.capturedPrompt().getOptions();
        Set<String> callbackNames = options.getToolCallbacks().stream()
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toSet());
        String renderedSystem = model.capturedPrompt().getSystemMessage().getText();
        String prefix = "registered tools (";
        int start = renderedSystem.indexOf(prefix) + prefix.length();
        int end = renderedSystem.indexOf("). The actor identity", start);
        Set<String> advertisedNames = Arrays.stream(renderedSystem.substring(start, end).split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        assertThat(callbackNames).hasSize(expectedSize).containsExactlyInAnyOrderElementsOf(expected);
        assertThat(advertisedNames).hasSize(expectedSize).containsExactlyInAnyOrderElementsOf(expected);
        assertThat(advertisedNames).isEqualTo(callbackNames);
        assertThat(List.copyOf(callbackNames))
                .noneMatch(name -> name.contains("delete") || name.contains("remove"));
    }

    @Configuration(proxyBeanMethods = false)
    static class Dependencies {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean(name = "openAiChatModel")
        com.ar.crm2.config.testing.CapturingChatModel openAiChatModel() {
            return new com.ar.crm2.config.testing.CapturingChatModel("ok");
        }
        @Bean SpringAiCrmTools springAiCrmTools(
                GetAllContactosUseCase getAllContactosUseCase,
                CreateContactoUseCase createContactoUseCase,
                EditContactoUseCase editContactoUseCase,
                CreateEmpresaUseCase createEmpresaUseCase,
                EditEmpresaUseCase editEmpresaUseCase,
                EditTratoUseCase editTratoUseCase) {
            return new SpringAiCrmTools(getAllContactosUseCase, createContactoUseCase, editContactoUseCase,
                    createEmpresaUseCase, editEmpresaUseCase, editTratoUseCase);
        }
        @Bean GetAllContactosUseCase getAllContactosUseCase() { return mock(GetAllContactosUseCase.class); }
        @Bean CreateContactoUseCase createContactoUseCase() { return mock(CreateContactoUseCase.class); }
        @Bean EditContactoUseCase editContactoUseCase() { return mock(EditContactoUseCase.class); }
        @Bean CreateEmpresaUseCase createEmpresaUseCase() { return mock(CreateEmpresaUseCase.class); }
        @Bean EditEmpresaUseCase editEmpresaUseCase() { return mock(EditEmpresaUseCase.class); }
        @Bean EditTratoUseCase editTratoUseCase() { return mock(EditTratoUseCase.class); }
        @Bean GetAllTablerosUseCase getAllTablerosUseCase() { return mock(GetAllTablerosUseCase.class); }
        @Bean GetTableroByIdUseCase getTableroByIdUseCase() { return mock(GetTableroByIdUseCase.class); }
        @Bean CreateTableroUseCase createTableroUseCase() { return mock(CreateTableroUseCase.class); }
        @Bean EditTableroUseCase editTableroUseCase() { return mock(EditTableroUseCase.class); }
        @Bean AsignarColumnaTableroUseCase asignarColumnaTableroUseCase() { return mock(AsignarColumnaTableroUseCase.class); }
        @Bean ReordenarColumnasUseCase reordenarColumnasUseCase() { return mock(ReordenarColumnasUseCase.class); }
        @Bean GetAllColumnasUseCase getAllColumnasUseCase() { return mock(GetAllColumnasUseCase.class); }
        @Bean GetColumnaByIdUseCase getColumnaByIdUseCase() { return mock(GetColumnaByIdUseCase.class); }
        @Bean CreateColumnaUseCase createColumnaUseCase() { return mock(CreateColumnaUseCase.class); }
        @Bean EditColumnaUseCase editColumnaUseCase() { return mock(EditColumnaUseCase.class); }
        @Bean GetAllFichasUseCase getAllFichasUseCase() { return mock(GetAllFichasUseCase.class); }
        @Bean GetFichaByIdUseCase getFichaByIdUseCase() { return mock(GetFichaByIdUseCase.class); }
        @Bean CreateFichaUseCase createFichaUseCase() { return mock(CreateFichaUseCase.class); }
        @Bean EditFichaUseCase editFichaUseCase() { return mock(EditFichaUseCase.class); }
        @Bean MoverColumnaFichaUseCase moverColumnaFichaUseCase() { return mock(MoverColumnaFichaUseCase.class); }
    }
}
