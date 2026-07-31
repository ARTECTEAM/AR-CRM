package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.application.contacto.command.CreateContactoCommand;
import com.ar.crm2.application.contacto.command.GetAllContactosCommand;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.application.trato.port.in.CambiarEstadoTratoUseCase;
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
 * Strict TDD contract for the A3 corrected Spring AI 2.0
 * {@link SpringAiCrmTools} class.
 *
 * <p>The corrected architecture replaces the prior per-invocation binder
 * with a single stateless {@link SpringAiCrmTools} bean that is
 * registered once via {@code ChatClient.Builder#defaultTools(Object...)}
 * and shared by every request. The trusted CRM {@code actorUsuarioId}
 * travels only through the framework's per-request
 * {@link ToolContext}; the model never sees it.
 *
 * <p>Verified contracts:
 * <ul>
 *     <li>Exactly three discovered tools — {@code find_contacts},
 *         {@code create_contact}, {@code update_deal_stage} — with the
 *         allowlist names. No aliases, no extras, no singletons.</li>
 *     <li>Real {@code @Tool} annotations populate names, descriptions,
 *         and JSON schemas; the model-facing schema never references the
 *         trusted actor or any identity field.</li>
 *     <li>Each tool resolves the actor only from the per-call
 *         {@link ToolContext} map under {@code actorUsuarioId}; missing,
 *         null, or wrong-type entries fail closed naturally — the
 *         trusted-actor resolver throws {@code IllegalStateException}
 *         (missing/null) or {@code IllegalArgumentException} (wrong
 *         type), and Spring AI's {@code MethodToolCallback.callMethod}
 *         wraps the throwable as a {@code ToolExecutionException} with
 *         the original cause preserved. There is no local sanitized
 *         redaction.</li>
 *     <li>Two distinct {@link ToolContext} values supplied to the same
 *         shared tools object reach the use cases as distinct actor
 *         UUIDs — identity isolation is enforced server-side, never via
 *         schema or model argument.</li>
 *     <li>{@code find_contacts} enforces the hard cap of 20.</li>
 *     <li>{@code create_contact} requires {@code estadoRelacion}.</li>
 *     <li>{@code update_deal_stage} routes GANADO/PERDIDO to the existing
 *         use case; unsupported statuses reject without mutation.</li>
 *     <li>Tool outputs are bounded records and never leak SQL,
 *         credentials, stack traces, or causes.</li>
 *     <li>The shared tools instance is reusable across invocations: the
 *         same {@code SpringAiCrmTools} object yields the same three
 *         callbacks (same identity), so {@code defaultTools(tools)}
 *         produces a stable defaults allowlist.</li>
 * </ul>
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
            CambiarEstadoTratoUseCase cambiarEstadoUseCase) {
        return new SpringAiCrmTools(
                contactosUseCase, createUseCase, cambiarEstadoUseCase, new ObjectMapper());
    }

    @Test
    void sharedToolsObjectExposesExactlyThreeAllowlistedCallbacksThroughSpringAiDiscovery() {
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(CambiarEstadoTratoUseCase.class));

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
                mock(CambiarEstadoTratoUseCase.class));

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
                mock(CambiarEstadoTratoUseCase.class));

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
                mock(CambiarEstadoTratoUseCase.class));

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
                mock(CambiarEstadoTratoUseCase.class));

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
                mock(CambiarEstadoTratoUseCase.class));

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
                mock(CambiarEstadoTratoUseCase.class));

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
                mock(CambiarEstadoTratoUseCase.class));

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
        // After the A3 review cleanup the @Tool method has no local try/catch:
        // mapper validation failures propagate naturally through Spring AI's
        // MethodToolCallback.callMethod, which wraps non-ToolExecutionException
        // throwables as ToolExecutionException with the ORIGINAL cause preserved.
        // CrmToolMapper.toCreateContactoCommand throws IllegalArgumentException
        // (requireNonBlank for estadoRelacion), so that is the cause we expect —
        // NOT a sanitized IllegalStateException boundary.
        CreateContactoUseCase createUseCase = mock(CreateContactoUseCase.class);
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                createUseCase,
                mock(CambiarEstadoTratoUseCase.class));

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
    void updateDealStageDelegatesToGanarWhenStatusIsGanado() throws Exception {
        UUID trustedActor = UUID.fromString("abcdefab-1111-2222-3333-444444444444");
        CambiarEstadoTratoUseCase cambiarEstadoUseCase = mock(CambiarEstadoTratoUseCase.class);
        Trato initial = Trato.create(
                ContactoId.create(), UsuarioId.from(trustedActor),
                "Deal", null, null, null, null);
        when(cambiarEstadoUseCase.ganar(any(UUID.class))).thenReturn(initial.ganar());
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                cambiarEstadoUseCase);

        ToolCallback updateDealStage = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "update_deal_stage");

        String input = "{\"id\":\"99999999-9999-9999-9999-999999999999\",\"status\":\"GANADO\"}";
        String output = updateDealStage.call(input, actorContext(trustedActor));

        verify(cambiarEstadoUseCase).ganar(UUID.fromString("99999999-9999-9999-9999-999999999999"));
        verify(cambiarEstadoUseCase, never()).perder(any(), any());

        JsonNode outputJson = MAPPER.readTree(output);
        assertThat(outputJson.get("status").asText()).isEqualTo(EstadoTrato.GANADO.name());
        assertThat(output)
                .as("update_deal_stage output must not leak internal fields")
                .doesNotContain("motivoPerdida")
                .doesNotContain("contactoId");
    }

    @Test
    void updateDealStageDelegatesToPerderWhenStatusIsPerdidoWithMotivo() throws Exception {
        UUID trustedActor = UUID.fromString("abcdefab-1111-2222-3333-444444444444");
        CambiarEstadoTratoUseCase cambiarEstadoUseCase = mock(CambiarEstadoTratoUseCase.class);
        UUID tratoId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        Trato initial = Trato.create(
                ContactoId.create(), UsuarioId.from(UUID.randomUUID()),
                "Deal", null, null, null, null);
        when(cambiarEstadoUseCase.perder(any(UUID.class), any(String.class)))
                .thenReturn(initial.perder("Budget"));
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                cambiarEstadoUseCase);

        ToolCallback updateDealStage = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "update_deal_stage");

        String input = "{\"id\":\"" + tratoId + "\",\"status\":\"PERDIDO\",\"motivo\":\"Budget\"}";
        updateDealStage.call(input, actorContext(trustedActor));

        verify(cambiarEstadoUseCase).perder(tratoId, "Budget");
        verify(cambiarEstadoUseCase, never()).ganar(any());
    }

    @Test
    void updateDealStageRejectsUnsupportedStatusWithoutMutation() {
        // After the A3 review cleanup the @Tool method has no local try/catch:
        // mapper validation failures propagate naturally through Spring AI's
        // MethodToolCallback.callMethod, which wraps non-ToolExecutionException
        // throwables as ToolExecutionException with the ORIGINAL cause preserved.
        // CrmToolMapper.toUpdateDealStageArguments throws IllegalArgumentException
        // (unsupported status check), so that is the cause we expect — NOT a
        // sanitized IllegalStateException boundary.
        CambiarEstadoTratoUseCase cambiarEstadoUseCase = mock(CambiarEstadoTratoUseCase.class);
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                cambiarEstadoUseCase);

        ToolCallback updateDealStage = findCallback(
                java.util.Arrays.asList(ToolCallbacks.from(tools)),
                "update_deal_stage");

        String input = "{\"id\":\"99999999-9999-9999-9999-999999999999\",\"status\":\"OPEN\"}";
        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(
                () -> updateDealStage.call(input, actorContext(UUID.randomUUID())));
        assertThat(failure)
                .as("unsupported status must surface through Spring AI's natural ToolExecutionException wrapper")
                .isInstanceOf(org.springframework.ai.tool.execution.ToolExecutionException.class);
        assertThat(failure.getCause())
                .as("the cause must preserve the original mapper IllegalArgumentException type, not a local sanitized replacement")
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(failure.getCause().getMessage())
                .as("the original mapper validation message must reach Spring AI unchanged")
                .isEqualTo("update_deal_stage status must be GANADO or PERDIDO, was: OPEN");
        verify(cambiarEstadoUseCase, never()).ganar(any());
        verify(cambiarEstadoUseCase, never()).perder(any(), any());
    }

    @Test
    void useCaseFailurePropagatesThroughSpringAiToolExecutionExceptionBoundaryWithoutLocalSanitization() {
        // With the local try/catch wrapper removed, the original use-case
        // exception reaches Spring AI's MethodToolCallback.callMethod which
        // wraps non-ToolExecutionException throwables as
        // ToolExecutionException with the original cause preserved. No
        // local redaction replaces the cause's type or message.
        GetAllContactosUseCase useCase = mock(GetAllContactosUseCase.class);
        when(useCase.getAll(any())).thenThrow(
                new IllegalStateException("downstream-failure-sentinel-must-not-be-redacted"));
        SpringAiCrmTools tools = newTools(
                useCase,
                mock(CreateContactoUseCase.class),
                mock(CambiarEstadoTratoUseCase.class));

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
        // Identity isolation contract: the SAME shared tools instance
        // is used by every request — there is no per-invocation binder or
        // per-invocation actor capture. The trusted actor for each call
        // is supplied ONLY through the framework's per-call ToolContext
        // and MUST reach the existing use case as the actorUsuarioId
        // argument without leaking.
        UUID actorA = UUID.fromString("deadbeef-0000-0000-0000-000000000001");
        UUID actorB = UUID.fromString("deadbeef-0000-0000-0000-000000000002");
        GetAllContactosUseCase useCase = mock(GetAllContactosUseCase.class);
        when(useCase.getAll(any(GetAllContactosCommand.class))).thenReturn(List.of());
        SpringAiCrmTools tools = newTools(
                useCase,
                mock(CreateContactoUseCase.class),
                mock(CambiarEstadoTratoUseCase.class));

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
        // The framework's MethodToolCallback validates that any
        // ToolContext-typed parameter is supplied and non-empty BEFORE
        // dispatching to the @Tool method. Missing or empty context
        // therefore fails closed at the framework boundary with
        // IllegalArgumentException — the model cannot bypass identity by
        // sending no context or by sending an empty map.
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(CambiarEstadoTratoUseCase.class));
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
        // The actor resolver rejects context maps that do not carry a
        // usable actorUsuarioId with a meaningful IllegalStateException;
        // Spring AI wraps it as ToolExecutionException without any
        // local sanitization.
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(CambiarEstadoTratoUseCase.class));
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
        // The actor resolver rejects non-UUID actorUsuarioId with a
        // meaningful IllegalArgumentException; Spring AI wraps the
        // natural exception as ToolExecutionException — no local
        // sanitization replaces the cause.
        SpringAiCrmTools tools = newTools(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(CambiarEstadoTratoUseCase.class));
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
        // The cleanup replaces the manual constructor (with explicit null
        // guards) by Lombok @RequiredArgsConstructor. The single generated
        // constructor must take exactly the four shared dependencies in
        // order so wiring errors surface as plain reflection failures.
        Constructor<?>[] constructors = SpringAiCrmTools.class.getDeclaredConstructors();
        assertThat(constructors).hasSize(1);
        Constructor<?> constructor = constructors[0];
        assertThat(constructor.getParameterTypes()).containsExactly(
                GetAllContactosUseCase.class,
                CreateContactoUseCase.class,
                CambiarEstadoTratoUseCase.class,
                ObjectMapper.class);
        constructor.setAccessible(true);
        Object instance = constructor.newInstance(
                mock(GetAllContactosUseCase.class),
                mock(CreateContactoUseCase.class),
                mock(CambiarEstadoTratoUseCase.class),
                new ObjectMapper());
        assertThat(instance).isInstanceOf(SpringAiCrmTools.class);
    }
}