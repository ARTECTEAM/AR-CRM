package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.FindContactsOutput;
import com.ar.crm2.application.contacto.command.CreateContactoCommand;
import com.ar.crm2.application.contacto.command.EditContactoCommand;
import com.ar.crm2.application.contacto.command.GetAllContactosCommand;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.EditContactoUseCase;
import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.application.empresa.command.CreateEmpresaCommand;
import com.ar.crm2.application.empresa.command.EditEmpresaCommand;
import com.ar.crm2.application.empresa.port.in.CreateEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.EditEmpresaUseCase;
import com.ar.crm2.application.trato.command.EditTratoCommand;
import com.ar.crm2.application.trato.port.in.EditTratoUseCase;
import com.ar.crm2.application.tablero.command.CreateTableroCommand;
import com.ar.crm2.application.tablero.port.in.CreateTableroUseCase;
import com.ar.crm2.application.columna.command.CreateColumnaCommand;
import com.ar.crm2.application.columna.port.in.CreateColumnaUseCase;
import com.ar.crm2.application.ficha.port.in.EditFichaUseCase;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.entity.Empresa;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.entity.Tablero;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.enums.TipoContrato;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.enums.TipoColumna;
import com.ar.crm2.model.enums.TipoFicha;
import com.ar.crm2.model.vo.TableroId;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Spring AI 2.0 contract tests for the stateless CRM resource tool groups.
 * They prove the catalog, model-visible schema boundary,
 * trusted per-call identity, bounded outputs, and Application
 * delegation.
 */
class SpringAiCrmToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ACTOR_CONTEXT_KEY = ToolContextSupport.ACTOR_CONTEXT_KEY;

    @Test
    void typedCallbacksValidateNamesAndRequiredEnumsWithTrustedContext() throws Exception {
        CreateTableroUseCase board = mock(CreateTableroUseCase.class);
        CreateColumnaUseCase column = mock(CreateColumnaUseCase.class);
        var tools = toolsWithBoardWrites(board, column, mock(EditFichaUseCase.class));
        var callbacks = java.util.Arrays.asList(ToolCallbacks.from(tools));
        ToolCallback create = findCallback(callbacks, "create_tablero");
        for (String invalid : List.of("tareas", "TASKS", "99999", "", " ")) {
            String input = MAPPER.writeValueAsString(Map.of("nombre", "Board", "descripcion", "Description", "tipoTablero", invalid));
            assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> create.call(input, actorContext(UUID.randomUUID())))).isNotNull();
        }
        verify(board, never()).create(any());
        for (String input : List.of("{\"nombre\":\"Board\",\"descripcion\":\"Description\"}",
                "{\"nombre\":\"Board\",\"descripcion\":\"Description\",\"tipoTablero\":null}",
                "{\"nombre\":\"Board\",\"descripcion\":\"Description\",\"tipoTablero\":99999}")) {
            assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> create.call(input, actorContext(UUID.randomUUID())))).isNotNull();
        }
        verify(board, never()).create(any());
        UUID actor = UUID.randomUUID();
        create.call("{\"nombre\":\"Board\",\"descripcion\":\"Description\",\"tipoTablero\":\"TAREAS\"}", actorContext(actor));
        var command = ArgumentCaptor.forClass(CreateTableroCommand.class);
        verify(board).create(command.capture());
        assertThat(command.getValue().actorId()).isEqualTo(actor);
        assertThat(command.getValue().tipoTablero()).isEqualTo(TipoTablero.TAREAS);
        for (String ordinal : List.of("0", "\"0\"")) {
            create.call("{\"nombre\":\"Board\",\"descripcion\":\"Description\",\"tipoTablero\":" + ordinal + "}", actorContext(actor));
        }
        verify(board, org.mockito.Mockito.times(3)).create(any());
        ToolCallback createColumn = findCallback(callbacks, "create_columna");
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> createColumn.call(
                "{\"nombre\":\"Default\",\"tipoTablero\":\"TAREAS\",\"tipoColumna\":\"PREDETERMINADA\"}", actorContext(actor)))).isNotNull();
        verify(column, never()).create(any());
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> createColumn.call(
                "{\"nombre\":\"Default\",\"tipoTablero\":0,\"tipoColumna\":0}", actorContext(actor)))).isNotNull();
        verify(column, never()).create(any());
        createColumn.call("{\"nombre\":\"Default\",\"tipoTablero\":\"TAREAS\",\"tipoColumna\":\"PREDETERMINADA\"}",
                new ToolContext(Map.of(ACTOR_CONTEXT_KEY, actor, ToolContextSupport.SUPER_USUARIO_CONTEXT_KEY, actor)));
        verify(column).create(any());
    }

    @Test
    void typedFindContactsPreservesOptionalNullAndRelationshipFilter() {
        var contacts = mock(GetAllContactosUseCase.class);
        var tools = newTools(contacts, mock(CreateContactoUseCase.class), mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class), mock(EditEmpresaUseCase.class), mock(EditTratoUseCase.class));
        verify(contacts, never()).getAll(any());
        var callback = findCallback(java.util.Arrays.asList(ToolCallbacks.from(tools)), "find_contacts");
        for (String input : List.of("{}", "{\"estadoRelacion\":null}", "{\"estadoRelacion\":\"ACTIVO\"}")) {
            callback.call(input, actorContext(UUID.randomUUID()));
        }
        var commands = ArgumentCaptor.forClass(GetAllContactosCommand.class);
        org.mockito.Mockito.verify(contacts, org.mockito.Mockito.times(3)).getAll(commands.capture());
        assertThat(commands.getAllValues().stream().map(GetAllContactosCommand::estadoRelacion).toList())
                .containsExactly(null, null, "ACTIVO");
    }

    private static ToolCallback findCallback(List<ToolCallback> callbacks, String name) {
        return callbacks.stream()
                .filter(callback -> name.equals(callback.getToolDefinition().name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "expected discovered callback named " + name));
    }

    private static ToolContext actorContext(UUID actor) {
        return new ToolContext(Map.of(ACTOR_CONTEXT_KEY, actor));
    }

    private static Object[] newTools(
            GetAllContactosUseCase contactosUseCase,
            CreateContactoUseCase createUseCase,
            EditContactoUseCase editContactoUseCase,
            CreateEmpresaUseCase createEmpresaUseCase,
            EditEmpresaUseCase editEmpresaUseCase,
            EditTratoUseCase editTratoUseCase) {
        return newTools(contactosUseCase, createUseCase, editContactoUseCase,
                createEmpresaUseCase, editEmpresaUseCase, editTratoUseCase,
                mock(com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase.class));
    }

    private static Object[] newTools(
            GetAllContactosUseCase contactosUseCase,
            CreateContactoUseCase createUseCase,
            EditContactoUseCase editContactoUseCase,
            CreateEmpresaUseCase createEmpresaUseCase,
            EditEmpresaUseCase editEmpresaUseCase,
            EditTratoUseCase editTratoUseCase,
            com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase getAllEmpresasUseCase) {
        var getContactoByIdUseCase = mock(com.ar.crm2.application.contacto.port.in.GetContactoByIdUseCase.class);
        when(getContactoByIdUseCase.getById(any())).thenAnswer(invocation -> {
            var command = invocation.getArgument(0, com.ar.crm2.application.contacto.command.GetContactoByIdCommand.class);
            var now = java.time.LocalDateTime.now();
            return Contacto.reconstitute(ContactoId.from(command.id()),
                    EmpresaId.from(UUID.fromString("11111111-2222-3332-4444-555555555555")),
                    null, null, "Existing contact", "existing@example.com", "existing phone",
                    "Existing title", "Existing source", now, now, EstadoRelacion.PROSPECTO);
        });
        var getTratoByIdUseCase = mock(com.ar.crm2.application.trato.port.in.GetTratoByIdUseCase.class);
        when(getTratoByIdUseCase.getById(any())).thenAnswer(invocation -> {
            var command = invocation.getArgument(0, com.ar.crm2.application.trato.command.GetTratoByIdCommand.class);
            var now = java.time.LocalDateTime.now();
            return Trato.reconstitute(TratoId.from(command.id()),
                    ContactoId.from(UUID.fromString("11111111-2222-3332-4444-555555555555")),
                    UsuarioId.from(UUID.fromString("77777777-7777-7777-7777-777777777777")),
                    "Existing deal", new BigDecimal("3200.00"), 60, LocalDate.parse("2027-03-15"),
                    TipoContrato.OTRO, com.ar.crm2.model.enums.EstadoTrato.ABIERTO, now, now);
        });
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
                new ContactoTools(contactosUseCase, createUseCase, editContactoUseCase,
                        getContactoByIdUseCase,
                        mock(com.ar.crm2.application.contacto.port.in.DeleteContactoUseCase.class),
                        mock(com.ar.crm2.application.contacto.port.in.CambiarEstadoContactoUseCase.class)),
                new EmpresaTools(createEmpresaUseCase,
                        getAllEmpresasUseCase,
                        editEmpresaUseCase, mock(com.ar.crm2.application.empresa.port.in.DeleteEmpresaUseCase.class),
                        mock(com.ar.crm2.application.empresa.port.in.CambiarEstadoEmpresaUseCase.class)),
                new TratoTools(mock(com.ar.crm2.application.trato.port.in.CreateTratoUseCase.class),
                        mock(com.ar.crm2.application.trato.port.in.GetAllTratosUseCase.class),
                        getTratoByIdUseCase,
                        editTratoUseCase, mock(com.ar.crm2.application.trato.port.in.DeleteTratoUseCase.class)),
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

    @Test
    void resourceGroupsExposeExactlyFiftyAllowlistedCallbacksThroughSpringAiDiscovery() {
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback[] callbacks = ToolCallbacks.from(tools);

        Set<String> names = java.util.Arrays.stream(callbacks)
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toUnmodifiableSet());
        assertThat(names).containsExactlyInAnyOrder(
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
    void sharedToolsObjectIsReusableAcrossMultipleDiscoveryCallsAndYieldsSameCallbacks() {
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback[] first = ToolCallbacks.from(tools);
        ToolCallback[] second = ToolCallbacks.from(tools);

        assertThat(first).as("the same shared tools object must yield the same callbacks on every discovery").isNotNull();
        assertThat(second).isNotNull();
        Set<String> firstNames = java.util.Arrays.stream(first)
                .map(c -> c.getToolDefinition().name())
                .collect(Collectors.toUnmodifiableSet());
        Set<String> secondNames = java.util.Arrays.stream(second)
                .map(c -> c.getToolDefinition().name())
                .collect(Collectors.toUnmodifiableSet());
        assertThat(secondNames).isEqualTo(firstNames);
    }

    @Test
    void discoveredCallbacksCarryRealAnnotationMetadataAndGeneratedSchemas() throws Exception {
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback[] callbacks = ToolCallbacks.from(tools);

        Map<String, ToolDefinition> byName = java.util.Arrays.stream(callbacks).collect(Collectors.toUnmodifiableMap(
                c -> c.getToolDefinition().name(),
                c -> c.getToolDefinition(),
                (a, b) -> a));

        ToolDefinition find = byName.get("find_contacts");
        assertThat(find.description())
                .as("description comes from @Tool annotation")
                .contains("Search contacts");
        JsonNode findSchema = MAPPER.readTree(find.inputSchema());
        assertThat(findSchema.get("type").asText()).isEqualTo("object");
        assertThat(findSchema.get("properties"))
                .as("find_contacts schema must expose the filter properties")
                .isNotNull();
        assertThat(findSchema.get("properties").has("search")).isTrue();

        ToolDefinition create = byName.get("create_contact");
        assertThat(create.description())
                .as("create_contact description comes from @Tool annotation")
                .contains("Create a contact");
        JsonNode createSchema = MAPPER.readTree(create.inputSchema());
        assertThat(createSchema.get("required"))
                .as("create_contact must require empresaId, nombre, and estadoRelacion")
                .isNotNull();
        Set<String> required = new HashSet<>();
        createSchema.get("required").forEach(node -> required.add(node.asText()));
        assertThat(required).contains("empresaId", "nombre", "estadoRelacion");

        ToolDefinition editContact = byName.get("edit_contact");
        assertThat(editContact.description())
                .as("edit_contact description comes from @Tool annotation")
                .contains("Edit an existing contact");
        JsonNode editContactSchema = MAPPER.readTree(editContact.inputSchema());
        assertThat(editContactSchema.get("required"))
                .as("edit_contact must require id, nombre, and estadoRelacion")
                .isNotNull();
        Set<String> editContactRequired = new HashSet<>();
        editContactSchema.get("required").forEach(node -> editContactRequired.add(node.asText()));
        assertThat(editContactRequired).contains("id", "nombre", "estadoRelacion");
        // The contact's identity and original creator are preserved by the
        // canonical use case and MUST NOT be exposed as editable inputs.
        JsonNode editContactProperties = editContactSchema.get("properties");
        assertThat(editContactProperties.has("empresaId"))
                .as("edit_contact MUST NOT advertise empresaId as an editable field")
                .isFalse();
        assertThat(editContactProperties.has("creadoPor"))
                .as("edit_contact MUST NOT advertise creadoPor as an editable field")
                .isFalse();

        ToolDefinition createCompany = byName.get("create_company");
        assertThat(createCompany.description())
                .as("create_company description comes from @Tool annotation")
                .contains("Create a company");
        JsonNode createCompanySchema = MAPPER.readTree(createCompany.inputSchema());
        assertThat(createCompanySchema.get("required"))
                .as("create_company must require nombre")
                .isNotNull();
        Set<String> createCompanyRequired = new HashSet<>();
        createCompanySchema.get("required").forEach(node -> createCompanyRequired.add(node.asText()));
        assertThat(createCompanyRequired).contains("nombre");

        ToolDefinition editCompany = byName.get("edit_company");
        assertThat(editCompany.description())
                .as("edit_company description comes from @Tool annotation")
                .contains("Edit an existing company");
        JsonNode editCompanySchema = MAPPER.readTree(editCompany.inputSchema());
        assertThat(editCompanySchema.get("required"))
                .as("edit_company must require id and nombre")
                .isNotNull();
        Set<String> editCompanyRequired = new HashSet<>();
        editCompanySchema.get("required").forEach(node -> editCompanyRequired.add(node.asText()));
        assertThat(editCompanyRequired).contains("id", "nombre");

        ToolDefinition edit = byName.get("edit_trato");
        assertThat(edit.description())
                .as("edit_trato description comes from @Tool annotation")
                .contains("Edit an existing deal");
        JsonNode editSchema = MAPPER.readTree(edit.inputSchema());
        assertThat(editSchema.get("required"))
                .as("edit_trato must require id, responsableId, and nombre")
                .isNotNull();
        Set<String> editRequired = new HashSet<>();
        editSchema.get("required").forEach(node -> editRequired.add(node.asText()));
        assertThat(editRequired).contains("id", "responsableId", "nombre", "tipoContrato");
        assertThat(byName.get("create_trato").description())
                .contains("initial Ficha", "do not create a second card");
        // Non-editable deal state is not part of the edit contract.
        JsonNode editProperties = editSchema.get("properties");
        assertThat(editProperties.has("status"))
                .as("edit_trato MUST NOT advertise status as an editable field")
                .isFalse();
    }

    @Test
    void discoveredSchemasExcludeTheActorContextAndNeverExposeAnyIdentityField() throws Exception {
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback[] callbacks = ToolCallbacks.from(tools);

        for (ToolCallback callback : callbacks) {
            ToolDefinition definition = callback.getToolDefinition();
            String schema = definition.inputSchema();
            assertThat(schema)
                    .as("schema for %s must not expose actor identity", definition.name())
                    .doesNotContain(ACTOR_CONTEXT_KEY)
                    .doesNotContain("actorSubject")
                    .doesNotContain("ownerSubject")
                    .doesNotContain("creadoPor")
                    .doesNotContain("tenantId")
                    .doesNotContain("handle")
                    .doesNotContain("turnId")
                    .doesNotContain("ToolContext");
        }
    }

    @Test
    void findContactsResolvesTrustedActorFromPerCallToolContextAndAppliesCap20() throws Exception {
        UUID trustedActor = UUID.fromString("aaaa1111-2222-3333-4444-555566667777");
        GetAllContactosUseCase contactosUseCase = mock(GetAllContactosUseCase.class);
        when(contactosUseCase.getAll(any(GetAllContactosCommand.class))).thenReturn(List.of());
        Object[] tools = newTools(
                contactosUseCase,
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback findContacts = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "find_contacts");

        findContacts.call("{\"search\":\"acme\"}", actorContext(trustedActor));

        ArgumentCaptor<GetAllContactosCommand> captor =
                ArgumentCaptor.forClass(GetAllContactosCommand.class);
        verify(contactosUseCase).getAll(captor.capture());
        GetAllContactosCommand command = captor.getValue();
        assertThat(command.actorUsuarioId())
                .as("trusted actor must be resolved from ToolContext, never from model arguments")
                .isEqualTo(trustedActor);
        assertThat(command.search()).isEqualTo("acme");
        assertThat(command.maxResults())
                .as("the query fetches one sentinel row while model-visible output remains capped at 20")
                .isEqualTo(21);
    }

    @Test
    void findContactsEmptyResultReturnsBoundedEmptyContactsArray() throws Exception {
        GetAllContactosUseCase useCase = mock(GetAllContactosUseCase.class);
        when(useCase.getAll(any(GetAllContactosCommand.class))).thenReturn(List.of());
        Object[] tools = newTools(
                useCase,
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback findContacts = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "find_contacts");

        String output = findContacts.call("{}", actorContext(UUID.randomUUID()));
        JsonNode result = MAPPER.readTree(output);
        assertThat(result.has("contacts"))
                .as("output must be the bounded FindContactsOutput object")
                .isTrue();
        assertThat(result.get("contacts").isArray()).isTrue();
        assertThat(result.get("contacts")).isEmpty();
    }

    @Test
    void directToolMethodReturnsTypedBoundedOutput() {
        GetAllContactosUseCase useCase = mock(GetAllContactosUseCase.class);
        when(useCase.getAll(any(GetAllContactosCommand.class))).thenReturn(List.of());
        Object[] tools = newTools(
                useCase,
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        FindContactsOutput output = ((ContactoTools) tools[3]).findContacts(null, null, null, null, null,
                actorContext(UUID.randomUUID()));

        assertThat(output.contacts()).isEmpty();
        assertThat(output.returned()).isZero();
        assertThat(output.truncated()).isFalse();
    }

    @Test
    void findContactsNonEmptyResultReturnsBoundedBusinessOutputOnly() throws Exception {
        Contacto contact = Contacto.create(
                EmpresaId.from(UUID.randomUUID()),
                "Acme",
                null,
                EstadoRelacion.PROSPECTO,
                null, null, null, null, null);
        GetAllContactosUseCase useCase = mock(GetAllContactosUseCase.class);
        when(useCase.getAll(any(GetAllContactosCommand.class))).thenReturn(List.of(contact));
        Object[] tools = newTools(
                useCase,
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback findContacts = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "find_contacts");

        String output = findContacts.call("{}", actorContext(UUID.randomUUID()));
        JsonNode contacts = MAPPER.readTree(output).get("contacts");
        assertThat(contacts.isArray()).isTrue();
        assertThat(contacts).hasSize(1);
        assertThat(contacts.get(0).get("id").asText()).isEqualTo(contact.getId().value().toString());
        assertThat(contacts.get(0).get("nombre").asText()).isEqualTo("Acme");
        assertThat(contacts.get(0).get("estadoRelacion").asText()).isEqualTo("PROSPECTO");
        JsonNode result = MAPPER.readTree(output);
        assertThat(result.get("returned").asInt()).isEqualTo(1);
        assertThat(result.get("truncated").asBoolean()).isFalse();
        assertThat(output)
                .as("bounded output must not leak domain internals")
                .doesNotContain("creadoPor")
                .doesNotContain("actualizadoEn")
                .doesNotContain("responsableId")
                .doesNotContain("telefono")
                .doesNotContain("comoNosConocio");
    }

    @Test
    void createContactDelegatesToCreateContactoUseCaseWithActorResolvedFromToolContext() throws Exception {
        UUID trustedActor = UUID.fromString("cccccccc-1111-2222-3333-444444444444");
        CreateContactoUseCase createUseCase = mock(CreateContactoUseCase.class);
        Contacto created = Contacto.create(
                EmpresaId.from(UUID.fromString("11111111-2222-3332-4444-555555555555")),
                "Acme Inc",
                null,
                EstadoRelacion.PROSPECTO,
                null,
                UsuarioId.from(trustedActor),
                null, null, null);
        when(createUseCase.create(any(CreateContactoCommand.class))).thenReturn(created);
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                createUseCase,
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback createContact = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "create_contact");

        String input = "{\"empresaId\":\"11111111-2222-3332-4444-555555555555\","
                + "\"nombre\":\"Acme Inc\",\"estadoRelacion\":\"PROSPECTO\"}";
        String output = createContact.call(input, actorContext(trustedActor));

        ArgumentCaptor<CreateContactoCommand> captor =
                ArgumentCaptor.forClass(CreateContactoCommand.class);
        verify(createUseCase).create(captor.capture());
        CreateContactoCommand command = captor.getValue();
        assertThat(command.creadoPor())
                .as("trusted actor from per-call ToolContext must reach the use case")
                .isEqualTo(trustedActor);
        assertThat(command.empresaId()).isEqualTo(UUID.fromString("11111111-2222-3332-4444-555555555555"));
        assertThat(command.nombre()).isEqualTo("Acme Inc");
        assertThat(command.estadoRelacion()).isEqualTo(EstadoRelacion.PROSPECTO);

        JsonNode outputJson = MAPPER.readTree(output);
        assertThat(outputJson.get("nombre").asText()).isEqualTo("Acme Inc");
        assertThat(outputJson.get("estadoRelacion").asText()).isEqualTo("PROSPECTO");
        assertThat(output)
                .as("create_contact output must not leak identity fields")
                .doesNotContain("creadoPor")
                .doesNotContain("actualizadoEn");
    }

    @Test
    void createContactRejectsMissingRelationshipStateBeforeMutation() {
        // Spring AI preserves mapper validation through ToolExecutionException.
        CreateContactoUseCase createUseCase = mock(CreateContactoUseCase.class);
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                createUseCase,
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback createContact = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "create_contact");

        String input = "{\"empresaId\":\"11111111-2222-3332-4444-555555555555\",\"nombre\":\"Acme\"}";
        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
                () -> createContact.call(input, actorContext(UUID.randomUUID())));
        assertThat(failure)
                .as("Spring AI 2.0 wraps tool exceptions in ToolExecutionException")
                .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
        assertThat(failure.getMessage())
                .as("the wrapped message must be the original mapper validation message, not a sanitized replacement")
                .isEqualTo("create_contact estadoRelacion is required");
        assertThat(failure.getCause())
                .as("the cause must preserve the original mapper IllegalArgumentException type")
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(failure.getCause().getMessage())
                .as("the original mapper validation message must reach Spring AI unchanged")
                .isEqualTo("create_contact estadoRelacion is required");
        assertThat(new SafeToolExecutionExceptionProcessor(MAPPER).process(
                (org.springframework.ai.tool.execution.ToolExecutionException) failure))
                .contains("TOOL_VALIDATION_FAILED", "The tool input is invalid.")
                .doesNotContain("create_contact estadoRelacion is required")
                .doesNotContain("java.lang");
        verify(createUseCase, never()).create(any());
    }

    @Test
    void editContactDelegatesToCanonicalEditContactoUseCaseAndPreservesTrustedActorBoundary() throws Exception {
        UUID trustedActor = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        UUID contactoId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID responsableId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        EditContactoUseCase editContactoUseCase = mock(EditContactoUseCase.class);
        Contacto updated = Contacto.reconstitute(
                ContactoId.from(contactoId),
                EmpresaId.from(UUID.fromString("11111111-2222-3332-4444-555555555555")),
                UsuarioId.from(responsableId),
                UsuarioId.from(trustedActor),
                "Renamed Contact",
                "renamed@example.com",
                "+525500000001",
                "VP Sales",
                "Referral",
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now(),
                EstadoRelacion.ACTIVO);
        when(editContactoUseCase.edit(any(EditContactoCommand.class))).thenReturn(updated);
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                editContactoUseCase,
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback editContact = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "edit_contact");

        String input = "{\"id\":\"" + contactoId
                + "\",\"nombre\":\"Renamed Contact\""
                + ",\"correo\":\"renamed@example.com\""
                + ",\"estadoRelacion\":\"ACTIVO\""
                + ",\"responsableId\":\"" + responsableId + "\"}";
        String output = editContact.call(input, actorContext(trustedActor));

        ArgumentCaptor<EditContactoCommand> captor =
                ArgumentCaptor.forClass(EditContactoCommand.class);
        verify(editContactoUseCase).edit(captor.capture());
        EditContactoCommand command = captor.getValue();
        assertThat(command.id()).isEqualTo(contactoId);
        assertThat(command.nombre()).isEqualTo("Renamed Contact");
        assertThat(command.correo()).isEqualTo("renamed@example.com");
        assertThat(command.estadoRelacion()).isEqualTo(EstadoRelacion.ACTIVO);
        assertThat(command.responsableId()).isEqualTo(responsableId);
        assertThat(command.telefono()).isEqualTo("existing phone");
        assertThat(command.cargo()).isEqualTo("Existing title");
        assertThat(command.comoNosConocio()).isEqualTo("Existing source");

        JsonNode outputJson = MAPPER.readTree(output);
        assertThat(outputJson.get("id").asText()).isEqualTo(contactoId.toString());
        assertThat(outputJson.get("nombre").asText()).isEqualTo("Renamed Contact");
        assertThat(outputJson.get("correo").asText()).isEqualTo("renamed@example.com");
        assertThat(outputJson.get("estadoRelacion").asText()).isEqualTo("ACTIVO");
        assertThat(outputJson.get("responsableId").asText()).isEqualTo(responsableId.toString());
        assertThat(output)
                .as("edit_contact output must not leak creadoPor/audit fields")
                .doesNotContain("creadoPor")
                .doesNotContain("creadoEn")
                .doesNotContain("actualizadoEn")
                .doesNotContain("empresaId");
    }

    @Test
    void editContactRejectsMissingIdNombreAndEstadoRelacionBeforeMutation() {
        EditContactoUseCase editContactoUseCase = mock(EditContactoUseCase.class);
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                editContactoUseCase,
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback editContact = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "edit_contact");

        String validId = "99999999-9999-9999-9999-999999999999";
        List<String> invalidInputs = List.of(
                "{\"nombre\":\"x\",\"estadoRelacion\":\"ACTIVO\"}",
                "{\"id\":\"" + validId + "\",\"estadoRelacion\":\"ACTIVO\"}",
                "{\"id\":\"" + validId + "\",\"nombre\":\"x\"}",
                "{\"id\":\"" + validId + "\",\"nombre\":\"   \",\"estadoRelacion\":\"ACTIVO\"}"
        );
        for (String input : invalidInputs) {
            Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
                    () -> editContact.call(input, actorContext(UUID.randomUUID())));
            assertThat(failure)
                    .as("missing or blank required field in %s", input)
                    .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
            assertThat(failure.getCause()).isInstanceOf(IllegalArgumentException.class);
        }
        verify(editContactoUseCase, never()).edit(any());
    }

    @Test
    void editContactRejectsUnknownEstadoRelacionBeforeMutation() {
        EditContactoUseCase editContactoUseCase = mock(EditContactoUseCase.class);
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                editContactoUseCase,
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback editContact = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "edit_contact");

        String input = "{\"id\":\"99999999-9999-9999-9999-999999999999\","
                + "\"nombre\":\"Acme\",\"estadoRelacion\":\"NOT_A_STATE\"}";
        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
                () -> editContact.call(input, actorContext(UUID.randomUUID())));
        assertThat(failure)
                .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
        assertThat(failure.getCause()).isInstanceOf(tools.jackson.databind.exc.InvalidFormatException.class);
        assertThat(failure.getCause().getMessage()).contains("EstadoRelacion");
        verify(editContactoUseCase, never()).edit(any());
    }

    @Test
    void createCompanyDelegatesToCreateEmpresaUseCaseWithActorResolvedFromToolContext() throws Exception {
        UUID trustedActor = UUID.fromString("cccc2222-3333-4444-5555-666666666666");
        CreateEmpresaUseCase createEmpresaUseCase = mock(CreateEmpresaUseCase.class);
        Empresa created = Empresa.create(
                "Acme", "Software", "+525500000000",
                null, null, null, null,
                EstadoRelacion.ACTIVO,
                null, UsuarioId.from(trustedActor), null);
        when(createEmpresaUseCase.create(any(CreateEmpresaCommand.class))).thenReturn(created);
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                createEmpresaUseCase,
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback createCompany = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "create_company");

        String input = "{\"nombre\":\"Acme\",\"estadoRelacion\":\"ACTIVO\"}";
        String output = createCompany.call(input, actorContext(trustedActor));

        ArgumentCaptor<CreateEmpresaCommand> captor =
                ArgumentCaptor.forClass(CreateEmpresaCommand.class);
        verify(createEmpresaUseCase).create(captor.capture());
        CreateEmpresaCommand command = captor.getValue();
        assertThat(command.nombre()).isEqualTo("Acme");
        assertThat(command.estadoRelacion()).isEqualTo(EstadoRelacion.ACTIVO);
        assertThat(command.creadoPor())
                .as("trusted actor from per-call ToolContext must reach the use case as creadoPor")
                .isEqualTo(trustedActor);

        JsonNode outputJson = MAPPER.readTree(output);
        assertThat(outputJson.get("nombre").asText()).isEqualTo("Acme");
        assertThat(outputJson.get("estadoRelacion").asText()).isEqualTo("ACTIVO");
        assertThat(output)
                .as("create_company output must not leak identity/audit fields")
                .doesNotContain("creadoPor")
                .doesNotContain("creadoEn")
                .doesNotContain("actualizadoEn")
                .doesNotContain("paginaWeb")
                .doesNotContain("facebook")
                .doesNotContain("notas");
    }

    @Test
    void createCompanyRejectsMissingNombreAndUnknownEstadoRelacionBeforeMutation() {
        CreateEmpresaUseCase createEmpresaUseCase = mock(CreateEmpresaUseCase.class);
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                createEmpresaUseCase,
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback createCompany = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "create_company");

        Throwable missingNombre = org.assertj.core.api.Assertions.catchThrowable(
                () -> createCompany.call("{}", actorContext(UUID.randomUUID())));
        assertThat(missingNombre)
                .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
        assertThat(missingNombre.getCause()).isInstanceOf(IllegalArgumentException.class);
        assertThat(missingNombre.getCause().getMessage()).contains("create_company requires nombre");

        Throwable unknownEstado = org.assertj.core.api.Assertions.catchThrowable(
                () -> createCompany.call("{\"nombre\":\"Acme\",\"estadoRelacion\":\"BAD\"}",
                        actorContext(UUID.randomUUID())));
        assertThat(unknownEstado)
                .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
        assertThat(unknownEstado.getCause()).isInstanceOf(tools.jackson.databind.exc.InvalidFormatException.class);
        assertThat(unknownEstado.getCause().getMessage()).contains("EstadoRelacion");

        verify(createEmpresaUseCase, never()).create(any());
    }

    @Test
    void editCompanyDelegatesToCanonicalEditEmpresaUseCaseAndPreservesTrustedActorBoundary() throws Exception {
        UUID trustedActor = UUID.fromString("dddd3333-4444-5555-6666-777777777777");
        UUID companyId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        UUID responsableId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        EditEmpresaUseCase editEmpresaUseCase = mock(EditEmpresaUseCase.class);
        Empresa updated = Empresa.reconstitute(
                EmpresaId.from(companyId),
                "Renamed Co", "Software", "+525500000001",
                "https://renamed.example",
                null, null, null,
                EstadoRelacion.ACTIVO,
                UsuarioId.from(responsableId),
                UsuarioId.from(trustedActor),
                "notes",
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now());
        when(editEmpresaUseCase.edit(any(EditEmpresaCommand.class))).thenReturn(updated);
        var getAllEmpresasUseCase = mock(com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase.class);
        when(getAllEmpresasUseCase.getAll()).thenReturn(List.of(updated));
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                editEmpresaUseCase,
                mock(EditTratoUseCase.class),
                getAllEmpresasUseCase);

        ToolCallback editCompany = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "edit_company");

        String input = "{\"id\":\"" + companyId
                + "\",\"nombre\":\"Renamed Co\""
                + ",\"estadoRelacion\":\"ACTIVO\""
                + ",\"responsableId\":\"" + responsableId + "\"}";
        String output = editCompany.call(input, actorContext(trustedActor));

        ArgumentCaptor<EditEmpresaCommand> captor =
                ArgumentCaptor.forClass(EditEmpresaCommand.class);
        verify(editEmpresaUseCase).edit(captor.capture());
        EditEmpresaCommand command = captor.getValue();
        assertThat(command.id()).isEqualTo(companyId);
        assertThat(command.nombre()).isEqualTo("Renamed Co");
        assertThat(command.estadoRelacion()).isEqualTo(EstadoRelacion.ACTIVO);
        assertThat(command.responsableId()).isEqualTo(responsableId);
        assertThat(command.sector()).isEqualTo("Software");
        assertThat(command.telefono()).isEqualTo("+525500000001");
        assertThat(command.paginaWeb()).isEqualTo("https://renamed.example");
        assertThat(command.notas()).isEqualTo("notes");

        JsonNode outputJson = MAPPER.readTree(output);
        assertThat(outputJson.get("id").asText()).isEqualTo(companyId.toString());
        assertThat(outputJson.get("nombre").asText()).isEqualTo("Renamed Co");
        assertThat(outputJson.get("estadoRelacion").asText()).isEqualTo("ACTIVO");
        assertThat(outputJson.get("responsableId").asText()).isEqualTo(responsableId.toString());
        assertThat(output)
                .as("edit_company output must not leak creadoPor/audit fields or social handles")
                .doesNotContain("creadoPor")
                .doesNotContain("creadoEn")
                .doesNotContain("actualizadoEn")
                .doesNotContain("facebook")
                .doesNotContain("instagram")
                .doesNotContain("twitter");
    }

    @Test
    void editCompanyRejectsMissingIdAndNombreBeforeMutation() {
        EditEmpresaUseCase editEmpresaUseCase = mock(EditEmpresaUseCase.class);
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                editEmpresaUseCase,
                mock(EditTratoUseCase.class));

        ToolCallback editCompany = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "edit_company");

        String validId = "88888888-8888-8888-8888-888888888888";
        List<String> invalidInputs = List.of(
                "{\"nombre\":\"Co\"}",
                "{\"id\":\"" + validId + "\"}",
                "{\"id\":\"" + validId + "\",\"nombre\":\"   \"}"
        );
        for (String input : invalidInputs) {
            Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
                    () -> editCompany.call(input, actorContext(UUID.randomUUID())));
            assertThat(failure)
                    .as("missing or blank required field in %s", input)
                    .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
            assertThat(failure.getCause()).isInstanceOf(IllegalArgumentException.class);
        }
        verify(editEmpresaUseCase, never()).edit(any());
    }

    @Test
    void editTratoDelegatesToCanonicalEditTratoUseCase() throws Exception {
        // The canonical use case preserves the deal state while editing fields.
        UUID trustedActor = UUID.fromString("abcdefab-1111-2222-3333-444444444444");
        UUID tratoId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID responsableId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        EditTratoUseCase editTratoUseCase = mock(EditTratoUseCase.class);
        Trato updated = Trato.reconstitute(
                TratoId.from(tratoId),
                ContactoId.create(),
                UsuarioId.from(responsableId),
                "Renamed Deal",
                new BigDecimal("1500.00"),
                75,
                LocalDate.parse("2026-12-31"),
                TipoContrato.SERVICIO,
                com.ar.crm2.model.enums.EstadoTrato.ABIERTO,
                java.time.LocalDateTime.now(),
                null);
        when(editTratoUseCase.edit(any(EditTratoCommand.class))).thenReturn(updated);
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                editTratoUseCase);

        ToolCallback editTrato = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "edit_trato");

        String input = "{\"id\":\"" + tratoId
                + "\",\"responsableId\":\"" + responsableId
                + "\",\"nombre\":\"Renamed Deal\""
                + ",\"tipoContrato\":\"SERVICIO\"}";
        String output = editTrato.call(input, actorContext(trustedActor));

        ArgumentCaptor<EditTratoCommand> captor =
                ArgumentCaptor.forClass(EditTratoCommand.class);
        verify(editTratoUseCase).edit(captor.capture());
        EditTratoCommand command = captor.getValue();
        assertThat(command.id()).isEqualTo(tratoId);
        assertThat(command.responsableId()).isEqualTo(responsableId);
        assertThat(command.nombre()).isEqualTo("Renamed Deal");
        assertThat(command.valorEstimado()).isEqualByComparingTo(new BigDecimal("3200.00"));
        assertThat(command.probabilidad()).isEqualTo(60);
        assertThat(command.fechaCierreEsperada()).isEqualTo(LocalDate.parse("2027-03-15"));
        assertThat(command.tipoContrato()).isEqualTo(TipoContrato.SERVICIO);

        JsonNode outputJson = MAPPER.readTree(output);
        assertThat(outputJson.get("id").asText()).isEqualTo(tratoId.toString());
        assertThat(outputJson.get("nombre").asText()).isEqualTo("Renamed Deal");
        assertThat(outputJson.get("responsableId").asText()).isEqualTo(responsableId.toString());
        assertThat(outputJson.get("tipoContrato").asText()).isEqualTo("SERVICIO");
        // Non-editable deal state is not surfaced in the tool output.
        assertThat(output)
                .as("edit_trato output must not leak non-editable deal state")
                .doesNotContain("estado")
                .doesNotContain("creadoEn")
                .doesNotContain("actualizadoEn")
                .doesNotContain("contactoId");
    }

    @Test
    void editTratoRejectsMissingIdResponsableIdAndNombreBeforeMutation() {
        EditTratoUseCase editTratoUseCase = mock(EditTratoUseCase.class);
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                editTratoUseCase);

        ToolCallback editTrato = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "edit_trato");

        UUID validResponsable = UUID.fromString("77777777-7777-7777-7777-777777777777");
        String validId = "99999999-9999-9999-9999-999999999999";
        List<String> invalidInputs = List.of(
                "{\"responsableId\":\"" + validResponsable + "\",\"nombre\":\"x\"}",
                "{\"id\":\"" + validId + "\",\"nombre\":\"x\"}",
                "{\"id\":\"" + validId + "\",\"responsableId\":\"" + validResponsable + "\"}",
                "{\"id\":\"" + validId + "\",\"responsableId\":\"" + validResponsable + "\",\"nombre\":\"  \"}",
                "{\"id\":\"" + validId + "\",\"responsableId\":\"" + validResponsable + "\",\"nombre\":\"x\",\"tipoContrato\":null}"
        );
        for (String input : invalidInputs) {
            Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
                    () -> editTrato.call(input, actorContext(UUID.randomUUID())));
            assertThat(failure)
                    .as("missing or blank required field in %s", input)
                    .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
            assertThat(failure.getCause()).isInstanceOf(IllegalArgumentException.class);
        }
        verify(editTratoUseCase, never()).edit(any());
    }

    @Test
    void editTratoRejectsUnknownTipoContratoBeforeMutation() {
        EditTratoUseCase editTratoUseCase = mock(EditTratoUseCase.class);
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                editTratoUseCase);

        ToolCallback editTrato = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "edit_trato");

        String input = "{\"id\":\"99999999-9999-9999-9999-999999999999\","
                + "\"responsableId\":\"77777777-7777-7777-7777-777777777777\","
                + "\"nombre\":\"Deal\",\"tipoContrato\":\"NOT_A_TYPE\"}";
        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
                () -> editTrato.call(input, actorContext(UUID.randomUUID())));
        assertThat(failure)
                .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
        assertThat(failure.getCause()).isInstanceOf(tools.jackson.databind.exc.InvalidFormatException.class);
        assertThat(failure.getCause().getMessage()).contains("TipoContrato");
        verify(editTratoUseCase, never()).edit(any());
    }

    @Test
    void useCaseFailureIsRedactedByTheConfiguredExceptionProcessor() {
        GetAllContactosUseCase useCase = mock(GetAllContactosUseCase.class);
        when(useCase.getAll(any())).thenThrow(
                new IllegalStateException("downstream-failure-sentinel-must-not-be-redacted"));
        Object[] tools = newTools(
                useCase,
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback findContacts = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "find_contacts");

        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
                () -> findContacts.call("{}", actorContext(UUID.randomUUID())));
        assertThat(failure)
                .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
        String modelVisible = new SafeToolExecutionExceptionProcessor(MAPPER).process(
                (org.springframework.ai.tool.execution.ToolExecutionException) failure);
        assertThat(modelVisible)
                .contains("TOOL_EXECUTION_FAILED")
                .doesNotContain("downstream-failure-sentinel-must-not-be-redacted");
    }

    @Test
    void useCaseFailureIsRedactedForCompanyTools() {
        EditEmpresaUseCase useCase = mock(EditEmpresaUseCase.class);
        when(useCase.edit(any())).thenThrow(
                new EmpresaNotFoundExceptionSentinel("empresa-not-found-sentinel"));
        var getAllEmpresasUseCase = mock(com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase.class);
        var now = java.time.LocalDateTime.now();
        when(getAllEmpresasUseCase.getAll()).thenReturn(List.of(Empresa.reconstitute(
                EmpresaId.from(UUID.fromString("88888888-8888-8888-8888-888888888888")),
                "Existing company", null, null, null, null, null, null,
                null, null, null, null, now, now)));
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                useCase,
                mock(EditTratoUseCase.class),
                getAllEmpresasUseCase);

        ToolCallback editCompany = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "edit_company");

        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
                () -> editCompany.call("{\"id\":\"88888888-8888-8888-8888-888888888888\",\"nombre\":\"x\"}",
                        actorContext(UUID.randomUUID())));
        assertThat(failure)
                .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
        String modelVisible = new SafeToolExecutionExceptionProcessor(MAPPER).process(
                (org.springframework.ai.tool.execution.ToolExecutionException) failure);
        assertThat(modelVisible).contains("TOOL_EXECUTION_FAILED").doesNotContain("empresa-not-found-sentinel");
    }

    @Test
    void sharedToolsObjectIsolatesDifferentActorsAcrossPerCallToolContexts() throws Exception {
        // The shared instance receives actor identity only per call.
        UUID actorA = UUID.fromString("deadbeef-0000-0000-0000-000000000001");
        UUID actorB = UUID.fromString("deadbeef-0000-0000-0000-000000000002");
        GetAllContactosUseCase useCase = mock(GetAllContactosUseCase.class);
        when(useCase.getAll(any(GetAllContactosCommand.class))).thenReturn(List.of());
        Object[] tools = newTools(
                useCase,
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        ToolCallback findContacts = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "find_contacts");

        findContacts.call("{}", actorContext(actorA));
        findContacts.call("{}", actorContext(actorB));

        ArgumentCaptor<GetAllContactosCommand> captor =
                ArgumentCaptor.forClass(GetAllContactosCommand.class);
        verify(useCase, org.mockito.Mockito.times(2)).getAll(captor.capture());
        List<GetAllContactosCommand> commands = captor.getAllValues();
        assertThat(commands.get(0).actorUsuarioId()).isEqualTo(actorA);
        assertThat(commands.get(1).actorUsuarioId()).isEqualTo(actorB);
    }

    @Test
    void missingOrEmptyActorContextFailsClosedAtFrameworkBoundary() {
        // MethodToolCallback rejects absent/empty context before dispatch.
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));
        ToolCallback findContacts = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "find_contacts");

        Throwable noContext = org.assertj.core.api.Assertions.catchThrowable(
                () -> findContacts.call("{}"));
        assertThat(noContext)
                .as("missing ToolContext must fail closed at the framework boundary")
                .isInstanceOf(IllegalArgumentException.class);

        Throwable emptyContext = org.assertj.core.api.Assertions.catchThrowable(
                () -> findContacts.call("{}", new ToolContext(Map.of())));
        assertThat(emptyContext)
                .as("empty ToolContext map must fail closed at the framework boundary")
                .isInstanceOf(IllegalArgumentException.class);

        Throwable nullContextMap = org.assertj.core.api.Assertions.catchThrowable(
                () -> findContacts.call("{}", new ToolContext(null)));
        assertThat(nullContextMap)
                .as("null ToolContext map must fail closed — the framework's ToolContext constructor rejects null maps")
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void presentContextWithoutActorKeyFailsClosedThroughNaturalBoundary() {
        // A present context without a usable actor is wrapped naturally.
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));
        ToolCallback findContacts = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "find_contacts");

        Throwable nullActor = org.assertj.core.api.Assertions.catchThrowable(
                () -> findContacts.call("{}", new ToolContext(
                        java.util.Collections.singletonMap(ACTOR_CONTEXT_KEY, null))));
        assertThat(nullActor)
                .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
        assertThat(nullActor.getCause()).isInstanceOf(IllegalStateException.class);
        assertThat(nullActor.getCause().getMessage()).containsIgnoringCase("actorUsuarioId");
    }

    @Test
    void wrongTypeActorValueFailsClosedThroughNaturalBoundary() {
        // Non-UUID actor values fail closed and preserve the cause.
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));
        ToolCallback findContacts = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "find_contacts");

        Throwable wrongType = org.assertj.core.api.Assertions.catchThrowable(
                () -> findContacts.call("{}", new ToolContext(
                        java.util.Collections.singletonMap(ACTOR_CONTEXT_KEY, "not-a-uuid"))));
        assertThat(wrongType)
                .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
        assertThat(wrongType.getCause()).isInstanceOf(IllegalArgumentException.class);
        assertThat(wrongType.getCause().getMessage()).containsIgnoringCase("UUID");
    }

    @Test
    void everyNewAggregateToolRequiresActorAndDelegatesToItsInputPort() throws Exception {
        var tableros = mock(com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase.class);
        var tableroById = mock(com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase.class);
        var createTablero = mock(com.ar.crm2.application.tablero.port.in.CreateTableroUseCase.class);
        var editTablero = mock(com.ar.crm2.application.tablero.port.in.EditTableroUseCase.class);
        var assign = mock(com.ar.crm2.application.tablero.port.in.AsignarColumnaTableroUseCase.class);
        var reorder = mock(com.ar.crm2.application.tablero.port.in.ReordenarColumnasUseCase.class);
        var columnas = mock(com.ar.crm2.application.columna.port.in.GetAllColumnasUseCase.class);
        var columnaById = mock(com.ar.crm2.application.columna.port.in.GetColumnaByIdUseCase.class);
        var createColumna = mock(com.ar.crm2.application.columna.port.in.CreateColumnaUseCase.class);
        var editColumna = mock(com.ar.crm2.application.columna.port.in.EditColumnaUseCase.class);
        var fichas = mock(com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase.class);
        var fichaById = mock(com.ar.crm2.application.ficha.port.in.GetFichaByIdUseCase.class);
        var createFicha = mock(com.ar.crm2.application.ficha.port.in.CreateFichaUseCase.class);
        var editFicha = mock(com.ar.crm2.application.ficha.port.in.EditFichaUseCase.class);
        var moveFicha = mock(com.ar.crm2.application.ficha.port.in.MoverColumnaFichaUseCase.class);
        when(tableros.getAll()).thenReturn(List.of());
        when(columnas.getAll()).thenReturn(List.of());
        when(fichas.getAll()).thenReturn(List.of());
        UUID columnaId = UUID.randomUUID();
        String existingColor = "#A1B2C3";
        var existingColumna = com.ar.crm2.model.entity.Columna.reconstitute(
                com.ar.crm2.model.vo.ColumnaId.from(columnaId), "Column", existingColor,
                TipoTablero.TAREAS, TipoColumna.PERSONALIZADA, false);
        when(columnaById.getById(any())).thenReturn(existingColumna);
        TableroTools tableroTools = new TableroTools(tableros, tableroById, createTablero, editTablero,
                mock(com.ar.crm2.application.tablero.port.in.DeleteTableroUseCase.class),
                mock(com.ar.crm2.application.tablero.port.in.EliminarColumnaDelTableroUseCase.class), assign, reorder);
        ColumnaTools columnaTools = new ColumnaTools(createColumna, columnas, columnaById, editColumna,
                mock(com.ar.crm2.application.columna.port.in.DeleteColumnaUseCase.class));
        FichaTools fichaTools = new FichaTools(createFicha, fichas, fichaById, editFicha,
                mock(com.ar.crm2.application.ficha.port.in.DeleteFichaUseCase.class), moveFicha);
        ToolContext context = actorContext(UUID.randomUUID());

        tableroTools.listTableros(context);
        tableroTools.getTablero(UUID.randomUUID(), context);
        tableroTools.createTablero("Board", "Description", TipoTablero.TAREAS, context);
        tableroTools.editTablero(UUID.randomUUID(), "Board", "Description", context);
        tableroTools.assignColumnaToTablero(UUID.randomUUID(), UUID.randomUUID(), 1, null, BigDecimal.ZERO, context);
        tableroTools.reorderTableroColumns(UUID.randomUUID(), List.of(UUID.randomUUID()), context);
        columnaTools.listColumnas(context);
        columnaTools.getColumna(UUID.randomUUID(), context);
        columnaTools.createColumna("Column", null, TipoTablero.TAREAS, TipoColumna.PERSONALIZADA, context);
        columnaTools.editColumna(columnaId, "Column", null, TipoTablero.TAREAS, TipoColumna.PERSONALIZADA, context);
        fichaTools.listFichas(context);
        fichaTools.getFicha(UUID.randomUUID(), context);
        fichaTools.createFicha(UUID.randomUUID(), TipoFicha.TRATO, UUID.randomUUID(), null, List.of(), context);
        fichaTools.editFicha(UUID.randomUUID(), UUID.randomUUID(), TipoFicha.TRATO, UUID.randomUUID(), null, List.of(), context);
        fichaTools.moveFichaToColumna(UUID.randomUUID(), UUID.randomUUID(), context);

        verify(tableros).getAll();
        verify(tableroById).getById(any());
        verify(createTablero).create(any());
        verify(editTablero).edit(any());
        verify(assign).asignarColumna(any());
        verify(reorder).reordenar(any());
        verify(columnas).getAll();
        verify(columnaById, org.mockito.Mockito.times(2)).getById(any());
        verify(createColumna).create(any());
        ArgumentCaptor<com.ar.crm2.application.columna.command.EditColumnaCommand> editCommand =
                ArgumentCaptor.forClass(com.ar.crm2.application.columna.command.EditColumnaCommand.class);
        verify(editColumna).edit(editCommand.capture());
        assertThat(editCommand.getValue().color()).isEqualTo(existingColor);
        verify(fichas).getAll();
        verify(fichaById).getById(any());
        verify(createFicha).create(any());
        verify(editFicha).edit(any());
        verify(moveFicha).moverAColumna(any());
    }

    @Test
    void deleteToolsAreExposedOnlyAsResourceSpecificCallbacks() {
        Object[] tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));

        Set<String> names = java.util.Arrays.stream(ToolCallbacks.from(tools))
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toUnmodifiableSet());

        assertThat(names)
                .as("delete callbacks must use explicit resource-specific names")
                .contains("delete_contact", "delete_company", "delete_trato", "delete_tarea",
                        "delete_etiqueta", "delete_agenda", "delete_tablero", "delete_columna", "delete_ficha")
                .doesNotContain("delete_empresa");
    }

    @Test
    void eachResourceGroupHasOneDependencyConstructor() {
        for (Class<?> tools : List.of(TableroTools.class, ColumnaTools.class, FichaTools.class,
                ContactoTools.class, EmpresaTools.class, TratoTools.class, TareaTools.class,
                EtiquetaTools.class, AgendaTools.class)) {
            Constructor<?>[] constructors = tools.getDeclaredConstructors();
            assertThat(constructors).as("%s should have one resource constructor", tools.getSimpleName())
                    .hasSize(1);
        }
    }

    @Test
    void listTablerosMapsAndSerializesOnlyTheBoundedSummary() throws Exception {
        var getAllTableros = mock(com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase.class);
        List<Tablero> boards = java.util.stream.IntStream.rangeClosed(1, 51)
                .mapToObj(index -> {
                    Tablero board = mock(Tablero.class);
                    when(board.getId()).thenReturn(TableroId.from(new UUID(0, index)));
                    when(board.getNombre()).thenReturn("Board " + index);
                    when(board.getDescripcion()).thenReturn("Description " + index);
                    when(board.getTipoTablero()).thenReturn(TipoTablero.TAREAS);
                    when(board.getColumnasTablero()).thenReturn(List.of());
                    return board;
                }).toList();
        when(getAllTableros.getAll()).thenReturn(boards);
        Object[] tools = toolsWithBoardRead(getAllTableros);
        ToolCallback list = findCallback(java.util.Arrays.asList(ToolCallbacks.from(tools)), "list_tableros");

        JsonNode serialized = MAPPER.readTree(list.call("{}", actorContext(UUID.randomUUID())));

        assertThat(serialized.get("tableros")).hasSize(50);
        assertThat(serialized.get("total").asInt()).isEqualTo(51);
        assertThat(serialized.get("truncated").asBoolean()).isTrue();
        assertThat(serialized.get("tableros").get(0).get("nombre").asText()).isEqualTo("Board 1");
        assertThat(serialized.toString()).doesNotContain("creadoEn", "columnasTablero");
    }

    @Test
    void createBoardMapsFieldsAndTrustedActorWhileDefaultColumnUsesTrustedSuperUserClaim() {
        CreateTableroUseCase createTablero = mock(CreateTableroUseCase.class);
        CreateColumnaUseCase createColumna = mock(CreateColumnaUseCase.class);
        Object[] tools = toolsWithBoardWrites(createTablero, createColumna, mock(EditFichaUseCase.class));
        UUID actor = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID superUser = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        ToolContext context = new ToolContext(Map.of(ACTOR_CONTEXT_KEY, actor,
                ToolContextSupport.SUPER_USUARIO_CONTEXT_KEY, superUser));

        ((TableroTools) tools[0]).createTablero("Board", "Description", TipoTablero.TAREAS, context);
        ((ColumnaTools) tools[1]).createColumna("Default", "#112233", TipoTablero.TAREAS,
                TipoColumna.PREDETERMINADA, context);

        ArgumentCaptor<CreateTableroCommand> boardCommand = ArgumentCaptor.forClass(CreateTableroCommand.class);
        verify(createTablero).create(boardCommand.capture());
        assertThat(boardCommand.getValue().nombre()).isEqualTo("Board");
        assertThat(boardCommand.getValue().descripcion()).isEqualTo("Description");
        assertThat(boardCommand.getValue().tipoTablero()).isEqualTo(TipoTablero.TAREAS);
        assertThat(boardCommand.getValue().actorId()).isEqualTo(actor);
        ArgumentCaptor<CreateColumnaCommand> columnCommand = ArgumentCaptor.forClass(CreateColumnaCommand.class);
        verify(createColumna).create(columnCommand.capture());
        assertThat(columnCommand.getValue().superUsuarioId()).contains(superUser);
    }

    @Test
    void aggregateWriteValidationFailsBeforeUseCaseAndRequiresTrustedSuperUserForDefaultColumn() {
        CreateTableroUseCase createTablero = mock(CreateTableroUseCase.class);
        CreateColumnaUseCase createColumna = mock(CreateColumnaUseCase.class);
        EditFichaUseCase editFicha = mock(EditFichaUseCase.class);
        Object[] tools = toolsWithBoardWrites(createTablero, createColumna, editFicha);
        ToolContext actorOnly = actorContext(UUID.randomUUID());

        Throwable invalidBoard = org.assertj.core.api.Assertions.catchThrowable(
                () -> ((TableroTools) tools[0]).createTablero(" ", "Description", TipoTablero.TAREAS, actorOnly));
        assertThat(invalidBoard).isInstanceOf(SafeToolValidationException.class);
        Throwable missingSuperUser = org.assertj.core.api.Assertions.catchThrowable(
                () -> ((ColumnaTools) tools[1]).createColumna(
                        "Default", null, TipoTablero.TAREAS, TipoColumna.PREDETERMINADA, actorOnly));
        assertThat(missingSuperUser).isInstanceOf(SafeToolValidationException.class)
                .hasMessageContaining("trusted super-user claim");
        Throwable omittedLabels = org.assertj.core.api.Assertions.catchThrowable(
                () -> ((FichaTools) tools[2]).editFicha(UUID.randomUUID(), UUID.randomUUID(), TipoFicha.TRATO, UUID.randomUUID(), null, null,
                        actorOnly));
        assertThat(omittedLabels).isInstanceOf(SafeToolValidationException.class)
                .hasMessageContaining("requires etiquetaIds");
        verify(createTablero, never()).create(any());
        verify(createColumna, never()).create(any());
        verify(editFicha, never()).edit(any());
    }

    private static Object[] toolsWithBoardRead(
            com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase getAllTableros) {
        Object[] tools = newTools(mock(GetAllContactosUseCase.class), mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class), mock(CreateEmpresaUseCase.class), mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));
        tools[0] = new TableroTools(getAllTableros,
                mock(com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase.class),
                mock(CreateTableroUseCase.class), mock(com.ar.crm2.application.tablero.port.in.EditTableroUseCase.class),
                mock(com.ar.crm2.application.tablero.port.in.DeleteTableroUseCase.class),
                mock(com.ar.crm2.application.tablero.port.in.EliminarColumnaDelTableroUseCase.class),
                mock(com.ar.crm2.application.tablero.port.in.AsignarColumnaTableroUseCase.class),
                mock(com.ar.crm2.application.tablero.port.in.ReordenarColumnasUseCase.class));
        return tools;
    }

    private static Object[] toolsWithBoardWrites(CreateTableroUseCase createTablero,
            CreateColumnaUseCase createColumna, EditFichaUseCase editFicha) {
        Object[] tools = newTools(mock(GetAllContactosUseCase.class), mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class), mock(CreateEmpresaUseCase.class), mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class));
        tools[0] = new TableroTools(mock(com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase.class),
                mock(com.ar.crm2.application.tablero.port.in.GetTableroByIdUseCase.class), createTablero,
                mock(com.ar.crm2.application.tablero.port.in.EditTableroUseCase.class),
                mock(com.ar.crm2.application.tablero.port.in.DeleteTableroUseCase.class),
                mock(com.ar.crm2.application.tablero.port.in.EliminarColumnaDelTableroUseCase.class),
                mock(com.ar.crm2.application.tablero.port.in.AsignarColumnaTableroUseCase.class),
                mock(com.ar.crm2.application.tablero.port.in.ReordenarColumnasUseCase.class));
        tools[1] = new ColumnaTools(createColumna, mock(com.ar.crm2.application.columna.port.in.GetAllColumnasUseCase.class),
                mock(com.ar.crm2.application.columna.port.in.GetColumnaByIdUseCase.class),
                mock(com.ar.crm2.application.columna.port.in.EditColumnaUseCase.class),
                mock(com.ar.crm2.application.columna.port.in.DeleteColumnaUseCase.class));
        tools[2] = new FichaTools(mock(com.ar.crm2.application.ficha.port.in.CreateFichaUseCase.class),
                mock(com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase.class),
                mock(com.ar.crm2.application.ficha.port.in.GetFichaByIdUseCase.class), editFicha,
                mock(com.ar.crm2.application.ficha.port.in.DeleteFichaUseCase.class),
                mock(com.ar.crm2.application.ficha.port.in.MoverColumnaFichaUseCase.class));
        return tools;
    }

    /**
     * Local exception type for the boundary-propagation test. Distinct from
     * the canonical {@code EmpresaNotFoundException} to keep the test
     * independent of Application type changes; the canonical exception may
     * evolve over time without affecting this contract.
     */
    private static final class EmpresaNotFoundExceptionSentinel extends RuntimeException {
        EmpresaNotFoundExceptionSentinel(String message) {
            super(message);
        }
    }
}
