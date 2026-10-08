package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.application.security.AuthorizationCapabilities;
import com.ar.crm2.application.security.ResourceCapabilities;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AgentToolPermissionPolicyTest {
    private static final List<Class<?>> TOOL_GROUPS = List.of(
            AgendaTools.class, ColumnaTools.class, ContactoTools.class,
            EmpresaTools.class, EtiquetaTools.class, FichaTools.class,
            TableroTools.class, TareaTools.class, TratoTools.class);

    @Test
    void mapsEveryRegisteredToolAndNoUnregisteredToolFromTheCurrentFiftyToolCatalog() {
        Set<String> registered = TOOL_GROUPS.stream()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .map(method -> method.getAnnotation(Tool.class))
                .filter(java.util.Objects::nonNull)
                .map(Tool::name)
                .collect(Collectors.toSet());

        assertThat(registered).hasSize(50);
        assertThat(AgentToolPermissionPolicy.toolNames()).isEqualTo(registered);
    }

    @Test
    void contactSearchRequiresRelatedCompanyReadBeforeTheCallbackIsExposed() {
        AuthorizationCapabilities contactReadOnly = capabilities(
                RecursoCrm.CONTACTO, AccionCrm.LEER);
        Set<String> contactReadTools = AgentToolPermissionPolicy.allowedTools(contactReadOnly);

        assertThat(contactReadTools).doesNotContain("find_contacts", "get_contact", "edit_contact");

        Set<String> contactAndCompanyReadTools = AgentToolPermissionPolicy.allowedTools(capabilities(
                RecursoCrm.CONTACTO, AccionCrm.LEER,
                RecursoCrm.EMPRESA, AccionCrm.LEER));
        assertThat(contactAndCompanyReadTools).contains("find_contacts", "get_contact");
    }

    @Test
    void createToolsRequireEveryMandatoryRelatedActionAndColumnRead() {
        AuthorizationCapabilities missingCompanyRead = capabilities(
                RecursoCrm.CONTACTO, AccionCrm.CREAR,
                RecursoCrm.CONTACTO, AccionCrm.LEER,
                RecursoCrm.FICHA, AccionCrm.CREAR,
                RecursoCrm.TRATO, AccionCrm.LEER,
                RecursoCrm.COLUMNA, AccionCrm.LEER);
        assertThat(AgentToolPermissionPolicy.allowedTools(missingCompanyRead)).doesNotContain("create_contact");

        AuthorizationCapabilities missingColumnRead = capabilities(
                RecursoCrm.TRATO, AccionCrm.CREAR,
                RecursoCrm.CONTACTO, AccionCrm.LEER,
                RecursoCrm.FICHA, AccionCrm.CREAR,
                RecursoCrm.TRATO, AccionCrm.LEER);
        assertThat(AgentToolPermissionPolicy.allowedTools(missingColumnRead))
                .doesNotContain("create_trato", "create_ficha");

        Set<String> completeDependencies = AgentToolPermissionPolicy.allowedTools(capabilities(
                RecursoCrm.CONTACTO, AccionCrm.CREAR,
                RecursoCrm.CONTACTO, AccionCrm.LEER,
                RecursoCrm.EMPRESA, AccionCrm.LEER,
                RecursoCrm.TRATO, AccionCrm.CREAR,
                RecursoCrm.FICHA, AccionCrm.CREAR,
                RecursoCrm.TRATO, AccionCrm.LEER,
                RecursoCrm.COLUMNA, AccionCrm.LEER));
        assertThat(completeDependencies).contains("create_contact", "create_trato");
    }

    @Test
    void boardMutationsRequireColumnReadAndCapabilityDescriptionOmitsSpecificIds() {
        AuthorizationCapabilities boardUpdateOnly = new AuthorizationCapabilities(Map.of(
                RecursoCrm.TABLERO, capability(AlcanceCrm.TODO_COMPARTIDO,
                        Set.of(AccionCrm.ACTUALIZAR), Set.of(), Set.of())));
        AuthorizationCapabilities boardUpdateWithColumns = new AuthorizationCapabilities(Map.of(
                RecursoCrm.TABLERO, capability(AlcanceCrm.TABLEROS_PERMITIDOS,
                        Set.of(AccionCrm.ACTUALIZAR), Set.of(), Set.of()),
                RecursoCrm.COLUMNA, capability(AlcanceCrm.TODO_COMPARTIDO,
                        Set.of(AccionCrm.LEER), Set.of(), Set.of())));

        assertThat(AgentToolPermissionPolicy.allowedTools(boardUpdateOnly))
                .doesNotContain("reorder_tablero_columns");
        assertThat(AgentToolPermissionPolicy.allowedTools(boardUpdateWithColumns))
                .contains("reorder_tablero_columns");

        AuthorizationCapabilities scoped = new AuthorizationCapabilities(Map.of(
                RecursoCrm.TABLERO, capability(AlcanceCrm.TABLEROS_PERMITIDOS,
                        Set.of(AccionCrm.LEER), Set.of(GrupoCampoSensible.FINANCIERO), Set.of())));
        String description = AgentToolPermissionPolicy.describe(scoped);
        assertThat(description).contains("TABLERO", "read", "TABLEROS_PERMITIDOS", "FINANCIERO");
        assertThat(description).doesNotContain("idsPermitidos", "allowedBoardId");
    }

    @Test
    void missingCapabilitiesProduceNoToolsAndAnExplicitEmptyCapabilityDescription() {
        assertThat(AgentToolPermissionPolicy.allowedTools(null)).isEmpty();
        assertThat(AgentToolPermissionPolicy.allowedTools(AuthorizationCapabilities.none())).isEmpty();
        assertThat(AgentToolPermissionPolicy.describe(AuthorizationCapabilities.none()))
                .isEqualTo("No CRM tool capabilities are available for this turn.");
    }

    private static AuthorizationCapabilities capabilities(Object... pairs) {
        Map<RecursoCrm, ResourceCapabilities> resources = new EnumMap<>(RecursoCrm.class);
        for (int index = 0; index < pairs.length; index += 2) {
            RecursoCrm resource = (RecursoCrm) pairs[index];
            AccionCrm action = (AccionCrm) pairs[index + 1];
            ResourceCapabilities current = resources.get(resource);
            EnumSet<AccionCrm> actions = current == null || current.actions().isEmpty()
                    ? EnumSet.noneOf(AccionCrm.class) : EnumSet.copyOf(current.actions());
            actions.add(action);
            resources.put(resource, capability(AlcanceCrm.TODO_COMPARTIDO, actions, Set.of(), Set.of()));
        }
        return new AuthorizationCapabilities(resources);
    }

    private static ResourceCapabilities capability(AlcanceCrm scope, Set<AccionCrm> actions,
                                                   Set<GrupoCampoSensible> readable,
                                                   Set<GrupoCampoSensible> writable) {
        return new ResourceCapabilities(scope, actions, readable, writable);
    }
}
