package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.CreateContactOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FindContactsOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.UpdateDealStageOutput;
import com.ar.crm2.application.agent.tool.command.AgentCrmWriteCommand;
import com.ar.crm2.application.agent.tool.port.in.AgentCrmWriteUseCase;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
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
 * Shared Spring AI 2.0 CRM tools. Request identity is read only from
 * {@link ToolContext}; update_deal_stage delegates through the Application
 * orchestrator, which enforces strict deal ownership before mutation.
 */
@RequiredArgsConstructor
public class SpringAiCrmTools {

    static final String ACTOR_CONTEXT_KEY = "actorUsuarioId";
    static final String AGENT_OWNER_CONTEXT_KEY = "agentOwnerId";
    static final String TURN_CONTEXT_KEY = "turnId";

    private final GetAllContactosUseCase getAllContactosUseCase;
    private final CreateContactoUseCase createContactoUseCase;
    private final AgentCrmWriteUseCase agentCrmWriteUseCase;
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
                    + "The owner, actor, and turn identities are trusted and are NOT model-visible arguments. "
                    + "The deal's responsableId MUST equal the trusted actor or the mutation is rejected."
    )
    public String updateDealStage(
            @ToolParam(description = "Deal UUID.") UUID id,
            @ToolParam(description = "New deal status. One of GANADO, PERDIDO.") String status,
            @ToolParam(required = false, description = "Loss reason; required when status is PERDIDO.") String motivo,
            ToolContext toolContext
    ) throws JsonProcessingException {
        TrustedWriteContext trusted = requireTrustedWriteContext(toolContext);
        CrmToolMapper.UpdateDealStageArguments args =
                CrmToolMapper.toUpdateDealStageArguments(id, status, motivo);
        AgentCrmWriteCommand command = new AgentCrmWriteCommand.UpdateDealStage(
                trusted.ownerId(),
                trusted.actorUsuarioId(),
                trusted.turnId(),
                args.tratoId(),
                args.status(),
                args.motivo());
        Trato trato = agentCrmWriteUseCase.execute(command);
        return objectMapper.writeValueAsString(CrmToolMapper.toUpdateDealStageOutput(trato));
    }

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

    private static TrustedWriteContext requireTrustedWriteContext(ToolContext toolContext) {
        UUID actor = requireActor(toolContext);
        if (toolContext.getContext() == null) {
            throw new IllegalStateException("agentOwnerId is required");
        }
        Object rawOwner = toolContext.getContext().get(AGENT_OWNER_CONTEXT_KEY);
        if (!(rawOwner instanceof String ownerValue) || ownerValue.isBlank()) {
            throw new IllegalArgumentException(
                    "agentOwnerId must be a non-blank String, was: "
                            + (rawOwner == null ? "null" : rawOwner.getClass().getName()));
        }
        Object rawTurn = toolContext.getContext().get(TURN_CONTEXT_KEY);
        if (!(rawTurn instanceof UUID turnValue)) {
            throw new IllegalArgumentException(
                    "turnId must be a UUID, was: "
                            + (rawTurn == null ? "null" : rawTurn.getClass().getName()));
        }
        return new TrustedWriteContext(
                AgentOwnerId.from(ownerValue), actor, TurnId.from(turnValue));
    }

    private record TrustedWriteContext(AgentOwnerId ownerId, UUID actorUsuarioId, TurnId turnId) {
    }
}
