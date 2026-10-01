package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.CreateCompanyOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditCompanyOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.ResourceToolOutput;
import com.ar.crm2.application.empresa.command.CambiarEstadoEmpresaCommand;
import com.ar.crm2.application.empresa.command.DeleteEmpresaCommand;
import com.ar.crm2.application.empresa.port.in.CambiarEstadoEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.CreateEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.DeleteEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.EditEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.entity.Empresa;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.UUID;

/** Spring AI tools for the Empresa REST resource. Actor context is not target ownership authorization. */
@RequiredArgsConstructor
public class EmpresaTools {

    private final CreateEmpresaUseCase createEmpresaUseCase;
    private final GetAllEmpresasUseCase getAllEmpresasUseCase;
    private final EditEmpresaUseCase editEmpresaUseCase;
    private final DeleteEmpresaUseCase deleteEmpresaUseCase;
    private final CambiarEstadoEmpresaUseCase cambiarEstadoEmpresaUseCase;
    private final CrmAuthorization authorization;

    @Tool(name = "create_company", description = "Create a company. Required name must be supplied by the user; do not invent it. The actor identity is trusted server context and is not a model argument.")
    public CreateCompanyOutput createCompany(
            @ToolParam(description = "Company name; required, non-blank.") String nombre,
            @ToolParam(required = false, description = "Optional sector; omit when not supplied.") String sector,
            @ToolParam(required = false, description = "Optional phone; omit when not supplied.") String telefono,
            @ToolParam(required = false, description = "Optional website URL; omit when not supplied.") String paginaWeb,
            @ToolParam(required = false, description = "Optional Facebook profile URL; omit when not supplied.") String facebook,
            @ToolParam(required = false, description = "Optional Instagram profile URL; omit when not supplied.") String instagram,
            @ToolParam(required = false, description = "Optional Twitter profile URL; omit when not supplied.") String twitter,
            @ToolParam(required = false, description = "Optional relationship state; omit when not supplied.") EstadoRelacion estadoRelacion,
            @ToolParam(required = false, description = "Optional responsible user UUID; omit when not supplied.") UUID responsableId,
            @ToolParam(required = false, description = "Optional notes; omit when not supplied.") String notas,
            ToolContext toolContext) {
        UUID trustedActor = ToolContextSupport.requireActor(toolContext);
        Empresa created = createEmpresaUseCase.create(CrmToolMapper.toCreateEmpresaCommand(
                nombre, sector, telefono, paginaWeb, facebook, instagram, twitter,
                estadoRelacion, responsableId, notas, trustedActor));
        return CrmToolMapper.projectSensitiveFields(
                CrmToolMapper.toCreateCompanyOutput(created),
                RecursoCrm.EMPRESA,
                authorization.fieldPolicy(RecursoCrm.EMPRESA));
    }

    @Tool(name = "list_companies", description = "List CRM companies in stable ID order with bounded output. Actor context is implicit.")
    public ResourceToolOutput.Companies listCompanies(ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.projectSensitiveFields(
                CrmToolMapper.toCompaniesOutput(getAllEmpresasUseCase.getAll()),
                RecursoCrm.EMPRESA,
                authorization.fieldPolicy(RecursoCrm.EMPRESA));
    }

    @Tool(name = "edit_company", description = "Edit an existing company's editable business fields. Its identity and original creator are not changed. responsableId is the business assignee, not the authenticated actor. Ask about missing required data; null/omitted optional values preserve their current values. Empty optional text clears that text; a nullable responsible-user ID cannot be cleared through this tool.")
    public EditCompanyOutput editCompany(
            @ToolParam(description = "Company UUID.") UUID id,
            @ToolParam(description = "Company name; required, non-blank.") String nombre,
            @ToolParam(required = false, description = "Optional sector; omit when not supplied.") String sector,
            @ToolParam(required = false, description = "Optional phone; omit when not supplied.") String telefono,
            @ToolParam(required = false, description = "Optional website URL; omit when not supplied.") String paginaWeb,
            @ToolParam(required = false, description = "Optional Facebook profile URL; omit when not supplied.") String facebook,
            @ToolParam(required = false, description = "Optional Instagram profile URL; omit when not supplied.") String instagram,
            @ToolParam(required = false, description = "Optional Twitter profile URL; omit when not supplied.") String twitter,
            @ToolParam(required = false, description = "Optional relationship state; omit when not supplied.") EstadoRelacion estadoRelacion,
            @ToolParam(required = false, description = "Optional responsible user UUID; omit when not supplied.") UUID responsableId,
            @ToolParam(required = false, description = "Optional notes; omit when not supplied.") String notas,
        ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        if (id == null) {
            throw new SafeToolValidationException("edit_company requires id");
        }
        String resolvedNombre = CrmToolMapper.requireNonBlank(nombre, "edit_company requires nombre");
        Empresa updated = editEmpresaUseCase.edit(CrmToolMapper.toEditEmpresaCommand(
                id, resolvedNombre,
                sector,
                telefono,
                paginaWeb,
                facebook,
                instagram,
                twitter,
                estadoRelacion,
                responsableId,
                notas));
        return CrmToolMapper.projectSensitiveFields(
                CrmToolMapper.toEditCompanyOutput(updated),
                RecursoCrm.EMPRESA,
                authorization.fieldPolicy(RecursoCrm.EMPRESA));
    }

    @Tool(name = "change_company_state", description = "Change a company's relationship state to the state explicitly selected by the user. Actor context is implicit.")
    public ResourceToolOutput.Company changeCompanyState(
            @ToolParam(description = "Company UUID.") UUID id,
            @ToolParam(description = "New relationship state.") EstadoRelacion nuevoEstado,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        return CrmToolMapper.projectSensitiveFields(
                CrmToolMapper.toCompanyOutput(cambiarEstadoEmpresaUseCase.cambiarEstado(
                        new CambiarEstadoEmpresaCommand(id, CrmToolMapper.requireEnum(nuevoEstado, "nuevoEstado")))),
                RecursoCrm.EMPRESA,
                authorization.fieldPolicy(RecursoCrm.EMPRESA));
    }

    @Tool(name = "delete_company", description = "Delete a company. This is destructive; call only when the user clearly requested deletion. Actor context is implicit.")
    public ResourceToolOutput.DeleteResult deleteCompany(
            @ToolParam(description = "Company UUID to delete.") UUID id,
            ToolContext toolContext) {
        ToolContextSupport.requireActor(toolContext);
        deleteEmpresaUseCase.delete(new DeleteEmpresaCommand(id));
        return new ResourceToolOutput.DeleteResult("empresa", id.toString(), true);
    }
}
