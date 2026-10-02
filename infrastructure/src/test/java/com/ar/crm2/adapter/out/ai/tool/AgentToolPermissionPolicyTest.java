package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.application.security.AuthorizationCapabilities;
import com.ar.crm2.application.security.ResourceCapabilities;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolPermissionPolicyTest {

    @Test
    void mapsEveryRegisteredToolAndNoUnregisteredTool() {
        Set<String> registered = Stream.of(
                        AgendaTools.class, ColumnaTools.class, ContactoTools.class,
                        EmpresaTools.class, EtiquetaTools.class, FichaTools.class,
                        TableroTools.class, TareaTools.class, TratoTools.class)
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .map(method -> method.getAnnotation(Tool.class))
                .filter(tool -> tool != null)
                .map(Tool::name)
                .collect(Collectors.toSet());

        assertEquals(registered, AgentToolPermissionPolicy.toolNames());
        assertEquals(50, registered.size());
    }

    @Test
    void filtersToolsByAllMandatoryDependenciesAndTypeDependentParentAlternatives() {
        AuthorizationCapabilities missingRelatedRead = capabilities(
                RecursoCrm.CONTACTO, AccionCrm.CREAR,
                RecursoCrm.FICHA, AccionCrm.CREAR,
                RecursoCrm.COLUMNA, AccionCrm.LEER,
                RecursoCrm.TRATO, AccionCrm.LEER);
        Set<String> withoutCompanyRead = AgentToolPermissionPolicy.allowedTools(missingRelatedRead);

        assertFalse(withoutCompanyRead.contains("create_contact"),
                "Create contact always verifies access to its selected company");
        assertFalse(withoutCompanyRead.contains("create_trato"),
                "Create deal always verifies contact and initial-card targets");
        assertTrue(withoutCompanyRead.contains("create_ficha"),
                "Ficha creation can target either an accessible deal or task");

        AuthorizationCapabilities withRequiredReads = capabilities(
                RecursoCrm.CONTACTO, AccionCrm.CREAR,
                RecursoCrm.CONTACTO, AccionCrm.LEER,
                RecursoCrm.EMPRESA, AccionCrm.LEER,
                RecursoCrm.TRATO, AccionCrm.CREAR,
                RecursoCrm.FICHA, AccionCrm.CREAR,
                RecursoCrm.COLUMNA, AccionCrm.LEER,
                RecursoCrm.TRATO, AccionCrm.LEER);
        Set<String> withCompanyRead = AgentToolPermissionPolicy.allowedTools(withRequiredReads);
        assertTrue(withCompanyRead.contains("create_contact"));
        assertTrue(withCompanyRead.contains("create_trato"));
    }

    @Test
    void reorderColumnsRequiresBothBoardUpdateAndColumnReadActions() {
        AuthorizationCapabilities boardUpdateOnly = capabilities(
                RecursoCrm.TABLERO, AccionCrm.ACTUALIZAR);
        AuthorizationCapabilities boardUpdateAndColumnRead = capabilities(
                RecursoCrm.TABLERO, AccionCrm.ACTUALIZAR,
                RecursoCrm.COLUMNA, AccionCrm.LEER);

        assertFalse(AgentToolPermissionPolicy.allowedTools(boardUpdateOnly)
                        .contains("reorder_tablero_columns"),
                "The use case reads every submitted column, so column-read is mandatory even though IDs vary");
        assertTrue(AgentToolPermissionPolicy.allowedTools(boardUpdateAndColumnRead)
                        .contains("reorder_tablero_columns"));
    }

    @Test
    void filtersSingleRecordToolsButKeepsUsefulQueriesThatMayReturnEmptyResults() {
        AuthorizationCapabilities contactReadOnly = capabilities(
                RecursoCrm.CONTACTO, AccionCrm.LEER);
        assertTrue(AgentToolPermissionPolicy.allowedTools(contactReadOnly).contains("find_contacts"),
                "Contact search remains useful as a query even if all rows are filtered by company scope");

        AuthorizationCapabilities dealReadOnly = capabilities(RecursoCrm.TRATO, AccionCrm.LEER);
        Set<String> withoutContactRead = AgentToolPermissionPolicy.allowedTools(dealReadOnly);
        assertFalse(withoutContactRead.contains("get_trato"));
        assertTrue(withoutContactRead.contains("list_tratos"),
                "Deal listing may return an empty authorized result set");
        assertFalse(AgentToolPermissionPolicy.allowedTools(capabilities(
                RecursoCrm.TRATO, AccionCrm.ACTUALIZAR)).contains("edit_trato"));
        Set<String> withContactRead = AgentToolPermissionPolicy.allowedTools(capabilities(
                RecursoCrm.TRATO, AccionCrm.LEER,
                RecursoCrm.TRATO, AccionCrm.ACTUALIZAR,
                RecursoCrm.CONTACTO, AccionCrm.LEER));
        assertTrue(withContactRead.contains("get_trato"));
        assertTrue(withContactRead.contains("edit_trato"));

        assertTrue(AgentToolPermissionPolicy.allowedTools(
                capabilities(RecursoCrm.TAREA, AccionCrm.LEER)).contains("list_tareas"),
                "Task listing may return an empty authorized result set");
        Set<String> fichaReadOnly = AgentToolPermissionPolicy.allowedTools(
                capabilities(RecursoCrm.FICHA, AccionCrm.LEER));
        assertTrue(fichaReadOnly.contains("list_fichas"),
                "Card listing may return an empty authorized result set");
        assertFalse(fichaReadOnly.contains("get_ficha"),
                "A single card is statically unreadable without its column and linked parent action");
    }

    @Test
    void boardReadsRemainAvailableWithoutColumnReadForBoardsWithNoColumns() {
        Set<String> tools = AgentToolPermissionPolicy.allowedTools(
                capabilities(RecursoCrm.TABLERO, AccionCrm.LEER));

        assertTrue(tools.contains("list_tableros"));
        assertTrue(tools.contains("get_tablero"));
    }

    private static AuthorizationCapabilities capabilities(Object... pairs) {
        Map<RecursoCrm, ResourceCapabilities> resources = new java.util.EnumMap<>(RecursoCrm.class);
        for (int i = 0; i < pairs.length; i += 2) {
            RecursoCrm resource = (RecursoCrm) pairs[i];
            AccionCrm action = (AccionCrm) pairs[i + 1];
            ResourceCapabilities current = resources.get(resource);
            EnumSet<AccionCrm> actions = current == null
                    ? EnumSet.noneOf(AccionCrm.class)
                    : EnumSet.copyOf(current.actions());
            actions.add(action);
            resources.put(resource, new ResourceCapabilities(
                    AlcanceCrm.TODO_COMPARTIDO, actions, Set.of(), Set.of()));
        }
        return new AuthorizationCapabilities(resources);
    }
}
