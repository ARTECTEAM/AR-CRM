package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.application.agent.tool.command.AgentCrmWriteCommand;
import com.ar.crm2.application.agent.tool.port.in.AgentCrmWriteUseCase;
import com.ar.crm2.application.contacto.command.CreateContactoCommand;
import com.ar.crm2.application.contacto.command.GetAllContactosCommand;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.enums.EstadoTrato;
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
 * They prove the three-tool allowlist, model-visible schema boundary, trusted
 * per-call identity, bounded outputs, and Application delegation.
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

    private static ToolContext trustedWriteContext(String owner, UUID actor, UUID turn) {
        return new ToolContext(Map.of(
                "agentOwnerId", owner, ACTOR_CONTEXT_KEY, actor, "turnId", turn));
    }

    private static SpringAiCrmTools newTools(
            GetAllContactosUseCase contactosUseCase,
            CreateContactoUseCase createUseCase,
            AgentCrmWriteUseCase agentCrmWriteUseCase) {
        return new SpringAiCrmTools(
                contactosUseCase, createUseCase, agentCrmWriteUseCase, new ObjectMapper());
    }

    @Test
    void sharedToolsObjectExposesExactlyThreeAllowlistedCallbacksThroughSpringAiDiscovery() {
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(AgentCrmWriteUseCase.class));

        ToolCallback[] callbacks = ToolCallbacks.from(tools);

        Set<String> names = java.util.Arrays.stream(callbacks)
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toUnmodifiableSet());
        assertThat(names).containsExactlyInAnyOrder(
                "find_contacts", "create_contact", "update_deal_stage");
    }

    @Test
    void sharedToolsObjectIsReusableAcrossMultipleDiscoveryCallsAndYieldsSameCallbacks() {
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(AgentCrmWriteUseCase.class));

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
                mock(AgentCrmWriteUseCase.class));

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

        ToolDefinition update = byName.get("update_deal_stage");
        assertThat(update.description())
                .as("update_deal_stage description comes from @Tool annotation")
                .contains("Update a deal");
        JsonNode updateSchema = MAPPER.readTree(update.inputSchema());
        assertThat(updateSchema.get("required"))
                .as("update_deal_stage must require id and status")
                .isNotNull();
        Set<String> updateRequired = new HashSet<>();
        updateSchema.get("required").forEach(node -> updateRequired.add(node.asText()));
        assertThat(updateRequired).contains("id", "status");
    }

    @Test
    void discoveredSchemasExcludeTheActorContextAndNeverExposeAnyIdentityField() throws Exception {
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(AgentCrmWriteUseCase.class));

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
                mock(AgentCrmWriteUseCase.class));

        ToolCallback findContacts = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "find_contacts");

        findContacts.call("{\"search\":\"acme\"}", actorContext(trustedActor));

        org.mockito.ArgumentCaptor<GetAllContactosCommand> captor =
                org.mockito.ArgumentCaptor.forClass(GetAllContactosCommand.class);
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
                mock(AgentCrmWriteUseCase.class));

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
                mock(AgentCrmWriteUseCase.class));

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
                mock(AgentCrmWriteUseCase.class));

        ToolCallback createContact = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "create_contact");

        String input = "{\"empresaId\":\"11111111-2222-3332-4444-555555555555\","
                + "\"nombre\":\"Acme Inc\",\"estadoRelacion\":\"PROSPECTO\"}";
        String output = createContact.call(input, actorContext(trustedActor));

        org.mockito.ArgumentCaptor<CreateContactoCommand> captor =
                org.mockito.ArgumentCaptor.forClass(CreateContactoCommand.class);
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
                mock(AgentCrmWriteUseCase.class));

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
    void updateDealStageDelegatesToGanarThroughAgentCrmWriteUseCaseOrchestrator() throws Exception {
        String ownerValue = "actor-pr9c4-c1-updatedeal-owner";
        UUID trustedActor = UUID.fromString("abcdefab-1111-2222-3333-444444444444");
        UUID turnValue = UUID.fromString("00000000-0000-0000-0000-000000c1a001");
        AgentCrmWriteUseCase orchestrator = mock(AgentCrmWriteUseCase.class);
        Trato initial = Trato.create(
                ContactoId.create(), UsuarioId.from(trustedActor),
                "Deal", null, null, null, null);
        when(orchestrator.execute(any(AgentCrmWriteCommand.class))).thenReturn(initial.ganar());
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                orchestrator);

        ToolCallback updateDealStage = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "update_deal_stage");

        String input = "{\"id\":\"99999999-9999-9999-9999-999999999999\",\"status\":\"GANADO\"}";
        String output = updateDealStage.call(
                input, trustedWriteContext(ownerValue, trustedActor, turnValue));

        ArgumentCaptor<AgentCrmWriteCommand> captor =
                ArgumentCaptor.forClass(AgentCrmWriteCommand.class);
        verify(orchestrator).execute(captor.capture());
        AgentCrmWriteCommand forwarded = captor.getValue();
        assertThat(forwarded)
                .isInstanceOf(AgentCrmWriteCommand.UpdateDealStage.class);
        AgentCrmWriteCommand.UpdateDealStage dealCmd =
                (AgentCrmWriteCommand.UpdateDealStage) forwarded;
        assertThat(dealCmd.tratoId())
                .isEqualTo(UUID.fromString("99999999-9999-9999-9999-999999999999"));
        assertThat(dealCmd.status()).isEqualTo("GANADO");
        assertThat(dealCmd.motivo()).isNull();
        assertThat(dealCmd.actorUsuarioId()).isEqualTo(trustedActor);
        assertThat(dealCmd.ownerId().value()).isEqualTo(ownerValue);
        assertThat(dealCmd.turnId().value()).isEqualTo(turnValue);

        JsonNode outputJson = MAPPER.readTree(output);
        assertThat(outputJson.get("status").asText()).isEqualTo(EstadoTrato.GANADO.name());
        assertThat(output)
                .as("update_deal_stage output must not leak internal fields")
                .doesNotContain("motivoPerdida")
                .doesNotContain("contactoId");
    }

    @Test
    void updateDealStageDelegatesToPerderThroughAgentCrmWriteUseCaseOrchestrator() throws Exception {
        String ownerValue = "actor-pr9c4-c1-perdido-owner";
        UUID trustedActor = UUID.fromString("abcdefab-1111-2222-3333-444444444444");
        UUID turnValue = UUID.fromString("00000000-0000-0000-0000-000000c1a002");
        UUID tratoId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        AgentCrmWriteUseCase orchestrator = mock(AgentCrmWriteUseCase.class);
        Trato initial = Trato.create(
                ContactoId.create(), UsuarioId.from(UUID.randomUUID()),
                "Deal", null, null, null, null);
        when(orchestrator.execute(any(AgentCrmWriteCommand.class)))
                .thenReturn(initial.perder("Budget"));
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                orchestrator);

        ToolCallback updateDealStage = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "update_deal_stage");

        String input = "{\"id\":\"" + tratoId + "\",\"status\":\"PERDIDO\",\"motivo\":\"Budget\"}";
        updateDealStage.call(input, trustedWriteContext(ownerValue, trustedActor, turnValue));

        ArgumentCaptor<AgentCrmWriteCommand> captor =
                ArgumentCaptor.forClass(AgentCrmWriteCommand.class);
        verify(orchestrator).execute(captor.capture());
        AgentCrmWriteCommand forwarded = captor.getValue();
        assertThat(forwarded)
                .isInstanceOf(AgentCrmWriteCommand.UpdateDealStage.class);
        AgentCrmWriteCommand.UpdateDealStage dealCmd =
                (AgentCrmWriteCommand.UpdateDealStage) forwarded;
        assertThat(dealCmd.status()).isEqualTo("PERDIDO");
        assertThat(dealCmd.motivo()).isEqualTo("Budget");
        assertThat(dealCmd.tratoId()).isEqualTo(tratoId);
    }

    @Test
    void updateDealStageRejectsUnsupportedStatusBeforeOrchestratorInvocation() {
        // Mapper validation is preserved by Spring AI's natural wrapper.
        AgentCrmWriteUseCase orchestrator = mock(AgentCrmWriteUseCase.class);
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                orchestrator);

        ToolCallback updateDealStage = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "update_deal_stage");

        String input = "{\"id\":\"99999999-9999-9999-9999-999999999999\",\"status\":\"OPEN\"}";
        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
                () -> updateDealStage.call(
                        input,
                        trustedWriteContext(
                                "actor-pr9c4-c1-bad-status",
                                UUID.randomUUID(),
                                UUID.fromString("00000000-0000-0000-0000-000000c1a003"))));
        assertThat(failure)
                .as("unsupported status must surface through Spring AI's natural ToolExecutionException wrapper")
                .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
        assertThat(failure.getCause())
                .as("the cause must preserve the original mapper IllegalArgumentException type, not a local sanitized replacement")
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(failure.getCause().getMessage())
                .as("the original mapper validation message must reach Spring AI unchanged")
                .isEqualTo("update_deal_stage status must be GANADO or PERDIDO, was: OPEN");
        verify(orchestrator, never()).execute(any());
    }

    @Test
    void useCaseFailurePropagatesThroughSpringAiToolExecutionExceptionBoundaryWithoutLocalSanitization() {
        // No local catch means Spring AI preserves the original cause.
        GetAllContactosUseCase useCase = mock(GetAllContactosUseCase.class);
        when(useCase.getAll(any())).thenThrow(
                new IllegalStateException("downstream-failure-sentinel-must-not-be-redacted"));
        SpringAiCrmTools tools = newTools(
                useCase,
                mock(CreateContactoUseCase.class),
                mock(AgentCrmWriteUseCase.class));

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
    void sharedToolsObjectIsolatesDifferentActorsAcrossPerCallToolContexts() throws Exception {
        // The shared instance receives actor identity only per call.
        UUID actorA = UUID.fromString("deadbeef-0000-0000-0000-000000000001");
        UUID actorB = UUID.fromString("deadbeef-0000-0000-0000-000000000002");
        GetAllContactosUseCase useCase = mock(GetAllContactosUseCase.class);
        when(useCase.getAll(any(GetAllContactosCommand.class))).thenReturn(List.of());
        SpringAiCrmTools tools = newTools(
                useCase,
                mock(CreateContactoUseCase.class),
                mock(AgentCrmWriteUseCase.class));

        ToolCallback findContacts = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "find_contacts");

        findContacts.call("{}", actorContext(actorA));
        findContacts.call("{}", actorContext(actorB));

        org.mockito.ArgumentCaptor<GetAllContactosCommand> captor =
                org.mockito.ArgumentCaptor.forClass(GetAllContactosCommand.class);
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
                mock(AgentCrmWriteUseCase.class));
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
                mock(AgentCrmWriteUseCase.class));
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
                mock(AgentCrmWriteUseCase.class));
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
    void sharedToolsConstructorIsLombokGeneratedAndTakesExactlyFourSharedDependencies() throws Exception {
        // Constructor shape protects composition-root wiring.
        Constructor<?>[] constructors = SpringAiCrmTools.class.getDeclaredConstructors();
        assertThat(constructors).hasSize(1);
        Constructor<?> constructor = constructors[0];
        assertThat(constructor.getParameterTypes()).containsExactly(
                GetAllContactosUseCase.class,
                CreateContactoUseCase.class,
                AgentCrmWriteUseCase.class,
                ObjectMapper.class);
        constructor.setAccessible(true);
        Object instance = constructor.newInstance(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(AgentCrmWriteUseCase.class),
                new ObjectMapper());
        assertThat(instance).isInstanceOf(SpringAiCrmTools.class);
    }

    // C1: trusted context reaches the Application orchestrator before writes.

    @Test
    void updateDealStageForwardsTrustedOwnerActorAndTurnToAgentCrmWriteUseCase() throws Exception {
        String ownerValue = "actor-pr9c4-c1-orchestrator-owner";
        UUID actorValue = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        UUID turnValue = UUID.fromString("00000000-0000-0000-0000-000000c1ff01");
        UUID tratoId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        AgentCrmWriteUseCase orchestrator = mock(AgentCrmWriteUseCase.class);
        Trato existing = Trato.create(
                ContactoId.create(), UsuarioId.from(actorValue),
                "Deal", null, null, null, null);
        when(orchestrator.execute(any(AgentCrmWriteCommand.class))).thenReturn(existing.ganar());
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                orchestrator);

        ToolCallback updateDealStage = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "update_deal_stage");

        ToolContext contextWithTrust = new ToolContext(new java.util.HashMap<>(Map.of(
                "agentOwnerId", ownerValue,
                "actorUsuarioId", actorValue,
                "turnId", turnValue)));
        String input = "{\"id\":\"" + tratoId + "\",\"status\":\"GANADO\"}";
        updateDealStage.call(input, contextWithTrust);

        ArgumentCaptor<AgentCrmWriteCommand> captor =
                ArgumentCaptor.forClass(AgentCrmWriteCommand.class);
        verify(orchestrator).execute(captor.capture());
        AgentCrmWriteCommand forwarded = captor.getValue();
        assertThat(forwarded)
                .as("update_deal_stage MUST forward a sealed AgentCrmWriteCommand, "
                        + "not call the actor-free AgentCrmWriteUseCase directly")
                .isInstanceOf(AgentCrmWriteCommand.UpdateDealStage.class);
        AgentCrmWriteCommand.UpdateDealStage dealCmd =
                (AgentCrmWriteCommand.UpdateDealStage) forwarded;
        assertThat(dealCmd.ownerId())
                .as("trusted owner MUST travel into the orchestrator command")
                .isEqualTo(AgentOwnerId.from(ownerValue));
        assertThat(dealCmd.actorUsuarioId())
                .as("trusted actor UUID MUST travel into the orchestrator command")
                .isEqualTo(actorValue);
        assertThat(dealCmd.turnId())
                .as("trusted turn MUST travel into the orchestrator command")
                .isEqualTo(TurnId.from(turnValue));
        assertThat(dealCmd.tratoId()).isEqualTo(tratoId);
        assertThat(dealCmd.status()).isEqualTo("GANADO");
        assertThat(dealCmd.motivo()).isNull();
    }

    @Test
    void updateDealStagePropagatesOrchestratorDenialAsToolExecutionException() {
        UUID actor = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID tratoId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        AgentCrmWriteUseCase orchestrator = mock(AgentCrmWriteUseCase.class);
        when(orchestrator.execute(any(AgentCrmWriteCommand.class))).thenThrow(
                new com.ar.crm2.application.agent.tool.exception.DealNotOwnedByActorException(
                        tratoId, actor));
        ToolCallback update = findCallback(List.of(ToolCallbacks.from(newTools(
                mock(GetAllContactosUseCase.class), mock(CreateContactoUseCase.class), orchestrator))),
                "update_deal_stage");

        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
                () -> update.call("{\"id\":\"" + tratoId + "\",\"status\":\"GANADO\"}",
                        trustedWriteContext("owner", actor,
                                UUID.fromString("00000000-0000-0000-0000-000000c1ff02"))));
        assertThat(failure).isInstanceOf(
                org.springframework.ai.tool.execution.ToolExecutionException.class);
        assertThat(failure.getCause()).isInstanceOf(
                com.ar.crm2.application.agent.tool.exception.DealNotOwnedByActorException.class);
    }
    @Test
    void malformedTrustedWriteContextFailsClosedBeforeApplicationUseCaseInvocation() {
        UUID actor = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID turn = UUID.fromString("00000000-0000-0000-0000-000000c1ff03");
        AgentCrmWriteUseCase orchestrator = mock(AgentCrmWriteUseCase.class);
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class), mock(CreateContactoUseCase.class), orchestrator);
        ToolCallback update = findCallback(List.of(ToolCallbacks.from(tools)), "update_deal_stage");
        List<Map<String, Object>> invalidContexts = List.of(
                Map.of(ACTOR_CONTEXT_KEY, actor, "turnId", turn),
                Map.of("agentOwnerId", " ", ACTOR_CONTEXT_KEY, actor, "turnId", turn),
                Map.of("agentOwnerId", 42, ACTOR_CONTEXT_KEY, actor, "turnId", turn),
                Map.of("agentOwnerId", "owner", ACTOR_CONTEXT_KEY, actor),
                Map.of("agentOwnerId", "owner", ACTOR_CONTEXT_KEY, actor, "turnId", "bad"));

        for (Map<String, Object> invalidContext : invalidContexts) {
            Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
                    () -> update.call(
                            "{\"id\":\"99999999-9999-9999-9999-999999999999\",\"status\":\"GANADO\"}",
                            new ToolContext(invalidContext)));
            assertThat(failure).isInstanceOf(
                    org.springframework.ai.tool.execution.ToolExecutionException.class);
        }
        verify(orchestrator, never()).execute(any());
    }
}
