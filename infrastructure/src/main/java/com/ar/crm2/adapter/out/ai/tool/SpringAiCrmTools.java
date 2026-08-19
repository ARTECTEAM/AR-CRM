package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.CreateCompanyOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.CreateContactOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditCompanyOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditContactOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditTratoOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FindCompaniesOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FindContactsOutput;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.EditContactoUseCase;
import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.application.empresa.port.in.CreateEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.EditEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase;
import com.ar.crm2.application.trato.port.in.EditTratoUseCase;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.entity.Empresa;
import com.ar.crm2.model.entity.Trato;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Shared Spring AI 2.0 CRM tools. Every allowlisted tool delegates to
 * an existing Application use case; the agent layer never accesses
 * repositories or business logic directly. Identity pieces that the
 * model MUST NOT supply (the authenticated actor) reach each tool only
 * through the per-request {@link ToolContext}; they are absent from the
 * generated JSON schema.
 *
 * <p>The current allowlist exposes seven thin adapters:
 * {@code find_contacts}, {@code create_contact}, {@code edit_contact},
 * {@code find_companies}, {@code create_company}, {@code edit_company},
 * and {@code edit_trato}. Company deletion is intentionally NOT
 * exposed — no {@code delete_company} tool exists and none must be
 * added without re-opening the canonical authorization design.
 *
 * <p>{@code edit_trato} is the canonical write tool for deals. It is a
 * thin adapter that maps model-visible arguments to the existing
 * {@code EditTratoCommand} and delegates to {@link EditTratoUseCase}.
 * The canonical use case preserves the deal's stage and loss reason;
 * {@code edit_trato} therefore does NOT advertise status or motivo as
 * editable fields. The authenticated actor is required at the tool
 * boundary for audit context, but is NOT used to populate the editable
 * {@code responsableId} field — that is a business assignment, not the
 * authenticated user.
 *
 * <p>{@code edit_contact} and {@code edit_company} mirror the REST
 * surface exactly: they accept the editable business fields the
 * canonical edit use case persists and omit audit/owner identity. The
 * creator ({@code creadoPor}) is preserved by the canonical use cases.
 */
@RequiredArgsConstructor
public class SpringAiCrmTools {

    static final String ACTOR_CONTEXT_KEY = "actorUsuarioId";

    private final GetAllContactosUseCase getAllContactosUseCase;
    private final CreateContactoUseCase createContactoUseCase;
    private final EditContactoUseCase editContactoUseCase;
    private final GetAllEmpresasUseCase getAllEmpresasUseCase;
    private final CreateEmpresaUseCase createEmpresaUseCase;
    private final EditEmpresaUseCase editEmpresaUseCase;
    private final EditTratoUseCase editTratoUseCase;
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
            name = "edit_contact",
            description = "Edit an existing contact's editable business fields (name, email, "
                    + "relationship state, responsible user, phone, job title, or acquisition source). "
                    + "The contact's identity (id), company (empresaId), and original creator are NOT "
                    + "changed by this tool. "
                    + "The actor identity is trusted and is NOT a model-visible argument. "
                    + "responsableId is the contact's business responsible user (the editable field), "
                    + "NOT the authenticated actor; pass the user the contact should be reassigned to."
    )
    public String editContact(
            @ToolParam(description = "Contact UUID; required.") UUID id,
            @ToolParam(description = "Contact name; required, non-blank.") String nombre,
            @ToolParam(required = false, description = "Contact email; optional.") String correo,
            @ToolParam(description = "Relationship state (EstadoRelacion name).") String estadoRelacion,
            @ToolParam(required = false, description = "Responsible user UUID; optional.") UUID responsableId,
            @ToolParam(required = false, description = "Contact phone; optional.") String telefono,
            @ToolParam(required = false, description = "Contact job title; optional.") String cargo,
            @ToolParam(required = false, description = "Acquisition source; optional.") String comoNosConocio,
            ToolContext toolContext
    ) throws JsonProcessingException {
        requireActor(toolContext);
        Contacto updated = editContactoUseCase.edit(CrmToolMapper.toEditContactoCommand(
                id, nombre, correo, estadoRelacion,
                responsableId, telefono, cargo, comoNosConocio));
        return objectMapper.writeValueAsString(CrmToolMapper.toEditContactOutput(updated));
    }

    @Tool(
            name = "find_companies",
            description = "Search companies visible to the current CRM actor. "
                    + "All filters are optional; the actor scope is implicit and is NOT a model-visible argument. "
                    + "The canonical company search does not return a hard cap; the adapter applies "
                    + "the supplied filters exactly as documented and orders results as persisted."
    )
    public String findCompanies(
            @ToolParam(required = false, description = "Free-text search applied to company name and related fields.") String search,
            @ToolParam(required = false, description = "Relationship state filter (exact EstadoRelacion name).") String estadoRelacion,
            @ToolParam(required = false, description = "Sector filter (exact value).") String sector,
            @ToolParam(required = false, description = "Responsible user UUID filter.") UUID responsableId,
            @ToolParam(required = false, description = "Web filter; CON_WEB or SIN_WEB.") String web,
            ToolContext toolContext
    ) throws JsonProcessingException {
        UUID trustedActor = requireActor(toolContext);
        List<Empresa> companies = getAllEmpresasUseCase.getAll(
                CrmToolMapper.toFindCompaniesCriteria(
                        search, estadoRelacion, sector, responsableId, web,
                        trustedActor));
        FindCompaniesOutput output = CrmToolMapper.toFindCompaniesOutput(companies);
        return objectMapper.writeValueAsString(output);
    }

    @Tool(
            name = "create_company",
            description = "Create a new company. "
                    + "The actor identity is trusted and is NOT a model-visible argument. "
                    + "Existing CRM validation, permission, and audit checks apply."
    )
    public String createCompany(
            @ToolParam(description = "Company name; required, non-blank.") String nombre,
            @ToolParam(required = false, description = "Sector; optional.") String sector,
            @ToolParam(required = false, description = "Phone number; optional.") String telefono,
            @ToolParam(required = false, description = "Website URL; optional.") String paginaWeb,
            @ToolParam(required = false, description = "Facebook profile URL; optional.") String facebook,
            @ToolParam(required = false, description = "Instagram profile URL; optional.") String instagram,
            @ToolParam(required = false, description = "Twitter profile URL; optional.") String twitter,
            @ToolParam(required = false, description = "Relationship state (EstadoRelacion name); optional.") String estadoRelacion,
            @ToolParam(required = false, description = "Responsible user UUID; optional.") UUID responsableId,
            @ToolParam(required = false, description = "Free-form notes; optional.") String notas,
            ToolContext toolContext
    ) throws JsonProcessingException {
        UUID trustedActor = requireActor(toolContext);
        Empresa created = createEmpresaUseCase.create(
                CrmToolMapper.toCreateEmpresaCommand(
                        nombre, sector, telefono, paginaWeb,
                        facebook, instagram, twitter,
                        estadoRelacion, responsableId, notas,
                        trustedActor));
        return objectMapper.writeValueAsString(CrmToolMapper.toCreateCompanyOutput(created));
    }

    @Tool(
            name = "edit_company",
            description = "Edit an existing company's editable business fields (name, sector, phone, "
                    + "website, social profiles, relationship state, responsible user, notes). "
                    + "The company's identity (id) and original creator are NOT changed by this tool. "
                    + "The actor identity is trusted and is NOT a model-visible argument. "
                    + "responsableId is the company's business responsible user (the editable field), "
                    + "NOT the authenticated actor; pass the user the company should be reassigned to."
    )
    public String editCompany(
            @ToolParam(description = "Company UUID; required.") UUID id,
            @ToolParam(description = "Company name; required, non-blank.") String nombre,
            @ToolParam(required = false, description = "Sector; optional.") String sector,
            @ToolParam(required = false, description = "Phone number; optional.") String telefono,
            @ToolParam(required = false, description = "Website URL; optional.") String paginaWeb,
            @ToolParam(required = false, description = "Facebook profile URL; optional.") String facebook,
            @ToolParam(required = false, description = "Instagram profile URL; optional.") String instagram,
            @ToolParam(required = false, description = "Twitter profile URL; optional.") String twitter,
            @ToolParam(required = false, description = "Relationship state (EstadoRelacion name); optional.") String estadoRelacion,
            @ToolParam(required = false, description = "Responsible user UUID; optional.") UUID responsableId,
            @ToolParam(required = false, description = "Free-form notes; optional.") String notas,
            ToolContext toolContext
    ) throws JsonProcessingException {
        requireActor(toolContext);
        Empresa updated = editEmpresaUseCase.edit(CrmToolMapper.toEditEmpresaCommand(
                id, nombre, sector, telefono, paginaWeb,
                facebook, instagram, twitter,
                estadoRelacion, responsableId, notas));
        return objectMapper.writeValueAsString(CrmToolMapper.toEditCompanyOutput(updated));
    }

    @Tool(
            name = "edit_trato",
            description = "Edit an existing deal's editable business fields (name, estimated value, "
                    + "probability, expected close date, contract type, or responsible user). "
                    + "The deal's stage (estado) and loss reason (motivoPerdida) are NOT changed by "
                    + "this tool; closing a deal requires a dedicated follow-up flow outside this "
                    + "tool's surface. "
                    + "The actor identity is trusted and is NOT a model-visible argument. "
                    + "responsableId is the deal's business responsible user (the editable field), "
                    + "NOT the authenticated actor; pass the user the deal should be reassigned to."
    )
    public String editTrato(
            @ToolParam(description = "Deal UUID; required.") UUID id,
            @ToolParam(description = "Responsible user UUID; required by the canonical edit use case. "
                    + "This is the deal's business responsable, NOT the authenticated actor.") UUID responsableId,
            @ToolParam(description = "Deal name; required, non-blank.") String nombre,
            @ToolParam(required = false, description = "Estimated deal value; optional.") BigDecimal valorEstimado,
            @ToolParam(required = false, description = "Win probability percentage 0-100; optional.") Integer probabilidad,
            @ToolParam(required = false, description = "Expected close date (ISO local date); optional.") LocalDate fechaCierreEsperada,
            @ToolParam(required = false, description = "Contract type (TipoContrato name); optional.") String tipoContrato,
            ToolContext toolContext
    ) throws JsonProcessingException {
        requireActor(toolContext);
        Trato updated = editTratoUseCase.edit(CrmToolMapper.toEditTratoCommand(
                id, responsableId, nombre, valorEstimado, probabilidad, fechaCierreEsperada, tipoContrato));
        return objectMapper.writeValueAsString(CrmToolMapper.toEditTratoOutput(updated));
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
}