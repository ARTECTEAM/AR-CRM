package com.ar.crm2.application.empresa.service;

import com.ar.crm2.application.empresa.command.EditEmpresaCommand;
import com.ar.crm2.application.empresa.exception.EmpresaNotFoundException;
import com.ar.crm2.application.empresa.port.in.EditEmpresaUseCase;
import com.ar.crm2.application.empresa.port.out.FindEmpresaByIdPort;
import com.ar.crm2.application.empresa.port.out.SaveEmpresaPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Empresa;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.UsuarioId;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing EditEmpresaUseCase.
 * Orchestrates loading the aggregate, applying the immutable domain update, and saving.
 */
@RequiredArgsConstructor
public class EditEmpresaService implements EditEmpresaUseCase {

    private final FindEmpresaByIdPort findPort;
    private final SaveEmpresaPort savePort;
    private final CrmAuthorization authorization;

    @Override
    public Empresa edit(EditEmpresaCommand command) {
        authorization.require(RecursoCrm.EMPRESA, AccionCrm.ACTUALIZAR);
        authorization.requireRecord(RecursoCrm.EMPRESA, AccionCrm.ACTUALIZAR, command.id());
        EmpresaId empresaId = EmpresaId.from(command.id());

        Empresa existing = findPort.findById(empresaId)
                .orElseThrow(() -> EmpresaNotFoundException.forId(command.id()));

        EnumSet<GrupoCampoSensible> changedPrivateGroups = EnumSet.noneOf(GrupoCampoSensible.class);
        if (command.telefono() != null && !Objects.equals(command.telefono(), existing.getTelefono())) {
            changedPrivateGroups.add(GrupoCampoSensible.CONTACTO_PRIVADO);
        }
        if (command.notas() != null && !Objects.equals(command.notas(), existing.getNotas())) {
            changedPrivateGroups.add(GrupoCampoSensible.CONTACTO_PRIVADO);
        }
        if (!changedPrivateGroups.isEmpty()) {
            authorization.requireWritableGroups(RecursoCrm.EMPRESA, changedPrivateGroups);
        }

        Empresa updated = Empresa.reconstitute(
                existing.getId(),
                command.nombre(),
                command.sector() != null ? command.sector() : existing.getSector(),
                command.telefono() != null ? command.telefono() : existing.getTelefono(),
                command.paginaWeb() != null ? command.paginaWeb() : existing.getPaginaWeb(),
                command.facebook() != null ? command.facebook() : existing.getFacebook(),
                command.instagram() != null ? command.instagram() : existing.getInstagram(),
                command.twitter() != null ? command.twitter() : existing.getTwitter(),
                command.estadoRelacion() != null ? command.estadoRelacion() : existing.getEstadoRelacion(),
                command.responsableId() != null ? UsuarioId.from(command.responsableId()) : existing.getResponsableId(),
                existing.getCreadoPor(),
                command.notas() != null ? command.notas() : existing.getNotas(),
                existing.getCreadoEn(),
                LocalDateTime.now()
        );

        authorization.requireCandidate(RecursoCrm.EMPRESA, AccionCrm.ACTUALIZAR,
                new ResourceScopeCandidate(null, null,
                        updated.getResponsableId() == null ? null : updated.getResponsableId().value(),
                        updated.getCreadoPor() == null ? null : updated.getCreadoPor().value()));
        return savePort.save(updated);
    }
}
