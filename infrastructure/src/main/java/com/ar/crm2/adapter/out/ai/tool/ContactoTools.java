package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.CreateContactOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditContactOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FindContactsOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.ResourceToolOutput;
import com.ar.crm2.application.contacto.command.CambiarEstadoContactoCommand;
import com.ar.crm2.application.contacto.command.DeleteContactoCommand;
import com.ar.crm2.application.contacto.command.GetContactoByIdCommand;
import com.ar.crm2.application.contacto.port.in.CambiarEstadoContactoUseCase;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.contacto.port.in.DeleteContactoUseCase;
import com.ar.crm2.application.contacto.port.in.EditContactoUseCase;
import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.application.contacto.port.in.GetContactoByIdUseCase;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.enums.EstadoRelacion;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.UUID;

/** Spring AI tools for the Contacto REST resource. Actor context is not target ownership authorization. */
@RequiredArgsConstructor
public class ContactoTools {

    private final GetAllContactosUseCase getAllContactosUseCase;
    private final CreateContactoUseCase createContactoUseCase;
    private final EditContactoUseCase editContactoUseCase;
    private final GetContactoByIdUseCase getContactoByIdUseCase;
    private final DeleteContactoUseCase deleteContactoUseCase;
    private final CambiarEstadoContactoUseCase cambiarEstadoContactoUseCase;

    @Tool(name = "find_contacts", description = "Search contacts using optional filters. The current actor is supplied by trusted server context and is not a model-visible argument. Results are capped at 20.")
    public FindContactsOutput findContacts(
            @ToolParam(required = false, description = "Optional free-text search applied to contact name.") String search,
            @ToolParam(required = false, description = "Optional relationship state.") EstadoRelacion estadoRelacion,
            @ToolParam(required = false, description = "Optional company UUID filter.") UUID empresaId,
            @ToolParam(required = false, description = "Optional responsible user UUID filter.") UUID responsableId,
            @ToolParam(required = false, description = "Optional acquisition source filter.") String comoNosConocio,
            ToolContext toolContext) {
        UUID trustedActor = ToolContextSupport.requireActor(toolContext);
        List<Contacto> contacts = getAllContactosUseCase.getAll(CrmToolMapper.toGetAllContactosCommand(
                search, estadoRelacion, empresaId, responsableId, comoNosConocio, trustedActor));
        return CrmToolMapper.toFindContactsOutput(contacts);
    }

    @Tool(name = "create_contact", description = "Create a contact. Required fields must be supplied by the user; ask about missing required data. The actor identity is trusted server context and is not a model argument.")
    public CreateContactOutput createContact(
            @ToolParam(description = "Company UUID the contact belongs to.") UUID empresaId,
            @ToolParam(description = "Contact name; required, non-blank.") String nombre,
            @ToolParam(required = false, description = "Optional contact email; omit when not supplied.") String correo,
            @ToolParam(description = "Relationship state.") EstadoRelacion estadoRelacion,
            @ToolParam(required = false, description = "Optional responsible user UUID; omit when not supplied.") UUID responsableId,
            @ToolParam(required = false, description = "Optional phone; omit when not supplied.") String telefono,
            @ToolParam(required = false, description = "Optional job title; omit when not supplied.") String cargo,
            @ToolParam(required = false, description = "Optional acquisition source; omit when not supplied.") String comoNosConocio,
            ToolContext toolContext) {
        UUID trustedActor = ToolContextSupport.requireActor(toolContext);
        Contacto created = createContactoUseCase.create(CrmToolMapper.toCreateContactoCommand(
                empresaId, nombre, correo, estadoRelacion, responsableId, telefono, cargo,
                comoNosConocio, trustedActor));
        return CrmToolMapper.toCreateContactOutput(created);
    }

    @Tool(name = "get_contact", description = "Get a contact by UUID. Actor context is implicit.")
    public ResourceToolOutput.Contact getContact(
            @ToolParam(description = "Contact UUID.") UUID id,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toContactOutput(getContactoByIdUseCase.getById(new GetContactoByIdCommand(id)));
    }

    @Tool(name = "edit_contact", description = "Edit an existing contact's editable business fields. Its identity, company, and original creator are not changed. responsableId is the business assignee, not the authenticated actor. Ask instead of inventing required values; null/omitted optional values preserve their current values. Empty optional text clears that text; a nullable responsible-user ID cannot be cleared through this tool.")
    public EditContactOutput editContact(
            @ToolParam(description = "Contact UUID.") UUID id,
            @ToolParam(description = "Contact name; required, non-blank.") String nombre,
            @ToolParam(required = false, description = "Optional contact email; omit when not supplied.") String correo,
            @ToolParam(description = "Relationship state.") EstadoRelacion estadoRelacion,
            @ToolParam(required = false, description = "Optional business responsible user UUID; omit when not supplied.") UUID responsableId,
            @ToolParam(required = false, description = "Optional phone; omit when not supplied.") String telefono,
            @ToolParam(required = false, description = "Optional job title; omit when not supplied.") String cargo,
            @ToolParam(required = false, description = "Optional acquisition source; omit when not supplied.") String comoNosConocio,
        ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        if (id == null) {
            throw new SafeToolValidationException("edit_contact requires id");
        }
        String resolvedNombre = CrmToolMapper.requireNonBlank(nombre, "edit_contact requires nombre");
        EstadoRelacion resolvedEstado = CrmToolMapper.requireEnum(estadoRelacion, "edit_contact estadoRelacion");
        Contacto current = getContactoByIdUseCase.getById(new GetContactoByIdCommand(id));
        if (current == null) {
            throw new SafeToolValidationException("edit_contact requires an existing contact");
        }
        Contacto updated = editContactoUseCase.edit(CrmToolMapper.toEditContactoCommand(
                id, resolvedNombre,
                correo != null ? correo : current.getCorreo(),
                resolvedEstado,
                responsableId != null ? responsableId
                        : current.getResponsableId() == null ? null : current.getResponsableId().value(),
                telefono != null ? telefono : current.getTelefono(),
                cargo != null ? cargo : current.getCargo(),
                comoNosConocio != null ? comoNosConocio : current.getComoNosConocio()));
        return CrmToolMapper.toEditContactOutput(updated);
    }

    @Tool(name = "change_contact_state", description = "Change a contact's relationship state to the state explicitly selected by the user. Actor context is implicit.")
    public ResourceToolOutput.Contact changeContactState(
            @ToolParam(description = "Contact UUID.") UUID id,
            @ToolParam(description = "New relationship state.") EstadoRelacion nuevoEstado,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.toContactOutput(cambiarEstadoContactoUseCase.cambiarEstado(
                new CambiarEstadoContactoCommand(id, CrmToolMapper.requireEnum(nuevoEstado, "nuevoEstado"))));
    }

    @Tool(name = "delete_contact", description = "Delete a contact. This is destructive; call only when the user clearly requested deletion. Existing CRM checks apply. Actor context is implicit.")
    public ResourceToolOutput.DeleteResult deleteContact(
            @ToolParam(description = "Contact UUID to delete.") UUID id,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        deleteContactoUseCase.delete(new DeleteContactoCommand(id));
        return new ResourceToolOutput.DeleteResult("contacto", id.toString(), true);
    }
}
