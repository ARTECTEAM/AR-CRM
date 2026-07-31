package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.CreateContactOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FindContactsOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.UpdateDealStageOutput;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.application.trato.port.in.CambiarEstadoTratoUseCase;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.entity.Trato;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.UUID;

/**
 * A3 review cleanup stateless Spring AI 2.0 CRM tools.
 *
 * <p>Registered ONCE on the {@code ChatClient} builder via
 * {@code defaultTools(tools)} by {@code com.ar.crm2.config.AgentConfig}
 * and shared across every request. The Lombok-generated constructor
 * stores only shared dependencies (the existing Application use cases
 * and the Jackson {@link ObjectMapper}); it does NOT store or capture
 * any request-scoped actor identity.
 *
 * <p>The trusted CRM {@code actorUsuarioId} reaches each tool through
 * the framework's per-request {@link ToolContext} parameter. The model
 * never sees the actor: Spring AI 2.0's {@code JsonSchemaGenerator}
 * excludes any {@link ToolContext}-typed parameter from the generated
 * JSON schema, and the framework dispatches the context at invocation
 * time through {@code MethodToolCallback.buildMethodArguments}.
 *
 * <p>Failure boundary: actor validation and {@link CrmToolMapper}
 * validation still prevent use-case invocation when the input is
 * invalid. Every other failure propagates naturally into Spring AI's
 * {@code MethodToolCallback.callMethod}, which wraps non
 * {@code ToolExecutionException} throwables as
 * {@code ToolExecutionException} with the original cause preserved.
 */
@RequiredArgsConstructor
public class SpringAiCrmTools {

    static final String ACTOR_CONTEXT_KEY = "actorUsuarioId";

    private final GetAllContactosUseCase getAllContactosUseCase;
    private final CreateContactoUseCase createContactoUseCase;
    private final CambiarEstadoTratoUseCase cambiarEstadoTratoUseCase;
    private final ObjectMapper objectMapper;

    @Tool(
            name = "find_contacts",
            description = "Search contacts visible to the current CRM actor. "
                    + "All filters are optional; the actor scope is implicit and is NOT a model-visible argument. "
                    + "The result is capped to 20 contacts and is ordered by recency then stable id."
    )
    public String findContacts(
            @ToolParam(required = false, description = "Free-text search applied to contact name.") String search,
            @ToolParam(required = false, description = "Relationship state filter (exact EstadoRelacion name).") String estadoRelacion,
            @ToolParam(required = false, description = "Company UUID filter.") UUID empresaId,
            @ToolParam(required = false, description = "Responsible user UUID filter.") UUID responsableId,
            @ToolParam(required = false, description = "Acquisition source filter (exact value).") String comoNosConocio,
            ToolContext toolContext
    ) throws JsonProcessingException {
        UUID trustedActor = requireActor(toolContext);
        List<Contacto> contacts = getAllContactosUseCase.getAll(
                CrmToolMapper.toGetAllContactosCommand(
                        search, estadoRelacion, empresaId, responsableId, comoNosConocio,
                        trustedActor));
        FindContactsOutput output = CrmToolMapper.toFindContactsOutput(contacts);
        return objectMapper.writeValueAsString(output);
    }

    @Tool(
            name = "create_contact",
            description = "Create a new contact. "
                    + "The actor identity is trusted and is NOT a model-visible argument. "
                    + "Existing CRM validation, permission, and audit checks apply."
    )
    public String createContact(
            @ToolParam(description = "Company UUID the contact belongs to.") UUID empresaId,
            @ToolParam(description = "Contact name; required, non-blank.") String nombre,
            @ToolParam(required = false, description = "Contact email; optional.") String correo,
            @ToolParam(description = "Relationship state (EstadoRelacion name).") String estadoRelacion,
            @ToolParam(required = false, description = "Responsible user UUID; optional.") UUID responsableId,
            @ToolParam(required = false, description = "Contact phone; optional.") String telefono,
            @ToolParam(required = false, description = "Contact job title; optional.") String cargo,
            @ToolParam(required = false, description = "Acquisition source; optional.") String comoNosConocio,
            ToolContext toolContext
    ) throws JsonProcessingException {
        UUID trustedActor = requireActor(toolContext);
        Contacto created = createContactoUseCase.create(
                CrmToolMapper.toCreateContactoCommand(
                        empresaId, nombre, correo, estadoRelacion,
                        responsableId, telefono, cargo, comoNosConocio,
                        trustedActor));
        CreateContactOutput output = CrmToolMapper.toCreateContactOutput(created);
        return objectMapper.writeValueAsString(output);
    }

    @Tool(
            name = "update_deal_stage",
            description = "Update a deal's stage to GANADO (won) or PERDIDO (lost). "
                    + "PERDIDO requires a motivo. "
                    + "The actor identity is trusted and is NOT a model-visible argument. "
                    + "Existing CRM permission and ownership checks apply."
    )
    public String updateDealStage(
            @ToolParam(description = "Deal UUID.") UUID id,
            @ToolParam(description = "New deal status. One of GANADO, PERDIDO.") String status,
            @ToolParam(required = false, description = "Loss reason; required when status is PERDIDO.") String motivo,
            ToolContext toolContext
    ) throws JsonProcessingException {
        UUID trustedActor = requireActor(toolContext);
        CrmToolMapper.UpdateDealStageArguments args =
                CrmToolMapper.toUpdateDealStageArguments(id, status, motivo);
        Trato trato = "GANADO".equals(args.status())
                ? cambiarEstadoTratoUseCase.ganar(args.tratoId())
                : cambiarEstadoTratoUseCase.perder(args.tratoId(), args.motivo());
        UpdateDealStageOutput output = CrmToolMapper.toUpdateDealStageOutput(trato);
        return objectMapper.writeValueAsString(output);
    }

    /**
     * Resolves the trusted CRM {@code actorUsuarioId} from the framework
     * {@link ToolContext}. Missing, null, or wrong-type entries fail
     * closed with a meaningful message; the model MUST NOT be able to
     * bypass identity by supplying arguments, by sending no context, or
     * by sending an unusable context. Spring AI wraps the resulting
     * exception in {@code ToolExecutionException}.
     */
    private static UUID requireActor(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            throw new IllegalStateException("actorUsuarioId is required");
        }
        Object raw = toolContext.getContext().get(ACTOR_CONTEXT_KEY);
        if (raw == null) {
            throw new IllegalStateException("actorUsuarioId is required");
        }
        if (!(raw instanceof UUID uuid)) {
            throw new IllegalArgumentException(
                    "actorUsuarioId must be a UUID, was: " + raw.getClass().getName());
        }
        return uuid;
    }
}