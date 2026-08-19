package com.ar.crm2.adapter.out.ai.tool;

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
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.entity.Empresa;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.enums.TipoContrato;
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
 * Spring AI 2.0 contract tests for the shared, stateless CRM tool bean.
 * They prove the six-tool allowlist, model-visible schema boundary,
 * trusted per-call identity, bounded outputs, and Application
 * delegation.
 */
class SpringAiCrmToolsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ACTOR_CONTEXT_KEY = "actorUsuarioId";

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

    private static SpringAiCrmTools newTools(
            GetAllContactosUseCase contactosUseCase,
            CreateContactoUseCase createUseCase,
            EditContactoUseCase editContactoUseCase,
            CreateEmpresaUseCase createEmpresaUseCase,
            EditEmpresaUseCase editEmpresaUseCase,
            EditTratoUseCase editTratoUseCase) {
        return new SpringAiCrmTools(
                contactosUseCase, createUseCase, editContactoUseCase,
                createEmpresaUseCase, editEmpresaUseCase, editTratoUseCase,
                new ObjectMapper());
    }

    @Test
    void sharedToolsObjectExposesExactlySixAllowlistedCallbacksThroughSpringAiDiscovery() {
        SpringAiCrmTools tools = newTools(
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
                "find_contacts", "create_contact", "edit_contact",
                "create_company", "edit_company", "edit_trato");
    }

    @Test
    void sharedToolsObjectIsReusableAcrossMultipleDiscoveryCallsAndYieldsSameCallbacks() {
        SpringAiCrmTools tools = newTools(
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
        SpringAiCrmTools tools = newTools(
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
                .contains("Create a new contact");
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
                .contains("Create a new company");
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
        assertThat(editRequired).contains("id", "responsableId", "nombre");
        // Non-editable deal state is not part of the edit contract.
        JsonNode editProperties = editSchema.get("properties");
        assertThat(editProperties.has("status"))
                .as("edit_trato MUST NOT advertise status as an editable field")
                .isFalse();
    }

    @Test
    void discoveredSchemasExcludeTheActorContextAndNeverExposeAnyIdentityField() throws Exception {
        SpringAiCrmTools tools = newTools(
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
        SpringAiCrmTools tools = newTools(
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
                .as("the hard cap of 20 must be applied regardless of filter inputs")
                .isEqualTo(20);
    }

    @Test
    void findContactsEmptyResultReturnsBoundedEmptyContactsArray() throws Exception {
        GetAllContactosUseCase useCase = mock(GetAllContactosUseCase.class);
        when(useCase.getAll(any(GetAllContactosCommand.class))).thenReturn(List.of());
        SpringAiCrmTools tools = newTools(
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
    void findContactsNonEmptyResultReturnsBoundedBusinessOutputOnly() throws Exception {
        Contacto contact = Contacto.create(
                EmpresaId.from(UUID.randomUUID()),
                "Acme",
                null,
                EstadoRelacion.PROSPECTO,
                null, null, null, null, null);
        GetAllContactosUseCase useCase = mock(GetAllContactosUseCase.class);
        when(useCase.getAll(any(GetAllContactosCommand.class))).thenReturn(List.of(contact));
        SpringAiCrmTools tools = newTools(
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
        assertThat(contacts.get(0).get("nombre").asText()).isEqualTo("Acme");
        assertThat(contacts.get(0).get("estadoRelacion").asText()).isEqualTo("PROSPECTO");
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
        SpringAiCrmTools tools = newTools(
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
        SpringAiCrmTools tools = newTools(
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
                .isEqualTo("create_contact requires estadoRelacion");
        assertThat(failure.getCause())
                .as("the cause must preserve the original mapper IllegalArgumentException type")
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(failure.getCause().getMessage())
                .as("the original mapper validation message must reach Spring AI unchanged")
                .isEqualTo("create_contact requires estadoRelacion");
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
        SpringAiCrmTools tools = newTools(
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
        SpringAiCrmTools tools = newTools(
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
        SpringAiCrmTools tools = newTools(
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
        assertThat(failure.getCause()).isInstanceOf(IllegalArgumentException.class);
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
        SpringAiCrmTools tools = newTools(
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
        SpringAiCrmTools tools = newTools(
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
        assertThat(unknownEstado.getCause()).isInstanceOf(IllegalArgumentException.class);
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
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                editEmpresaUseCase,
                mock(EditTratoUseCase.class));

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
        SpringAiCrmTools tools = newTools(
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
        SpringAiCrmTools tools = newTools(
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
                + ",\"valorEstimado\":1500.00,\"probabilidad\":75"
                + ",\"fechaCierreEsperada\":\"2026-12-31\""
                + ",\"tipoContrato\":\"SERVICIO\"}";
        String output = editTrato.call(input, actorContext(trustedActor));

        ArgumentCaptor<EditTratoCommand> captor =
                ArgumentCaptor.forClass(EditTratoCommand.class);
        verify(editTratoUseCase).edit(captor.capture());
        EditTratoCommand command = captor.getValue();
        assertThat(command.id()).isEqualTo(tratoId);
        assertThat(command.responsableId()).isEqualTo(responsableId);
        assertThat(command.nombre()).isEqualTo("Renamed Deal");
        assertThat(command.valorEstimado()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(command.probabilidad()).isEqualTo(75);
        assertThat(command.fechaCierreEsperada()).isEqualTo(LocalDate.parse("2026-12-31"));
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
        SpringAiCrmTools tools = newTools(
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
                "{\"id\":\"" + validId + "\",\"responsableId\":\"" + validResponsable + "\",\"nombre\":\"  \"}"
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
        SpringAiCrmTools tools = newTools(
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
        assertThat(failure.getCause()).isInstanceOf(IllegalArgumentException.class);
        assertThat(failure.getCause().getMessage()).contains("TipoContrato");
        verify(editTratoUseCase, never()).edit(any());
    }

    @Test
    void useCaseFailurePropagatesThroughSpringAiToolExecutionExceptionBoundaryWithoutLocalSanitization() {
        // No local catch means Spring AI preserves the the original cause.
        GetAllContactosUseCase useCase = mock(GetAllContactosUseCase.class);
        when(useCase.getAll(any())).thenThrow(
                new IllegalStateException("downstream-failure-sentinel-must-not-be-redacted"));
        SpringAiCrmTools tools = newTools(
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
        assertThat(failure.getCause())
                .as("the cause must preserve the original use-case exception type, not a local sanitized replacement")
                .isInstanceOf(IllegalStateException.class);
        assertThat(failure.getCause().getMessage())
                .as("the original cause message must reach Spring AI unchanged")
                .contains("downstream-failure-sentinel-must-not-be-redacted");
    }

    @Test
    void useCaseFailurePropagatesUnchangedForCompanyTools() {
        // Same boundary guarantee applies to every company write tool.
        EditEmpresaUseCase useCase = mock(EditEmpresaUseCase.class);
        when(useCase.edit(any())).thenThrow(
                new EmpresaNotFoundExceptionSentinel("empresa-not-found-sentinel"));
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                useCase,
                mock(EditTratoUseCase.class));

        ToolCallback editCompany = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "edit_company");

        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
                () -> editCompany.call("{\"id\":\"88888888-8888-8888-8888-888888888888\",\"nombre\":\"x\"}",
                        actorContext(UUID.randomUUID())));
        assertThat(failure)
                .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
        assertThat(failure.getCause())
                .isInstanceOf(EmpresaNotFoundExceptionSentinel.class);
        assertThat(failure.getCause().getMessage()).contains("empresa-not-found-sentinel");
    }

    @Test
    void sharedToolsObjectIsolatesDifferentActorsAcrossPerCallToolContexts() throws Exception {
        // The shared instance receives actor identity only per call.
        UUID actorA = UUID.fromString("deadbeef-0000-0000-0000-000000000001");
        UUID actorB = UUID.fromString("deadbeef-0000-0000-0000-000000000002");
        GetAllContactosUseCase useCase = mock(GetAllContactosUseCase.class);
        when(useCase.getAll(any(GetAllContactosCommand.class))).thenReturn(List.of());
        SpringAiCrmTools tools = newTools(
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
        SpringAiCrmTools tools = newTools(
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
        SpringAiCrmTools tools = newTools(
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
        SpringAiCrmTools tools = newTools(
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
    void noCompanyDeleteToolIsExposedByTheSharedSpringAiCrmToolsBean() {
        // Defence-in-depth: explicit allowlist check, in addition to the
        // generic six-tool discovery assertion, that no tool that even
        // hints at company deletion is exposed.
        SpringAiCrmTools tools = newTools(
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
                .as("no company-delete tool may be exposed to the LLM")
                .noneMatch(name -> name.toLowerCase().contains("delete"))
                .doesNotContain("delete_company")
                .doesNotContain("delete_empresa");
    }

    @Test
    void sharedToolsConstructorIsLombokGeneratedAndTakesExactlySevenSharedDependencies() throws Exception {
        // Constructor shape protects composition-root wiring.
        Constructor<?>[] constructors = SpringAiCrmTools.class.getDeclaredConstructors();
        assertThat(constructors).hasSize(1);
        Constructor<?> constructor = constructors[0];
        assertThat(constructor.getParameterTypes()).containsExactly(
                GetAllContactosUseCase.class,
                CreateContactoUseCase.class,
                EditContactoUseCase.class,
                CreateEmpresaUseCase.class,
                EditEmpresaUseCase.class,
                EditTratoUseCase.class,
                ObjectMapper.class);
        constructor.setAccessible(true);
        Object instance = constructor.newInstance(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(EditContactoUseCase.class),
                mock(CreateEmpresaUseCase.class),
                mock(EditEmpresaUseCase.class),
                mock(EditTratoUseCase.class),
                new ObjectMapper());
        assertThat(instance).isInstanceOf(SpringAiCrmTools.class);
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
