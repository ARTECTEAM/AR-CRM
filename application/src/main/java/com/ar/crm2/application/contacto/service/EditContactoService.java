package com.ar.crm2.application.contacto.service;

import com.ar.crm2.application.contacto.command.EditContactoCommand;
import com.ar.crm2.application.contacto.exception.ContactoNotFoundException;
import com.ar.crm2.application.contacto.port.in.EditContactoUseCase;
import com.ar.crm2.application.contacto.port.out.FindContactoByIdPort;
import com.ar.crm2.application.contacto.port.out.SaveContactoPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Application service implementing EditContactoUseCase.
 * Orchestrates loading the aggregate, applying the immutable domain update, and saving.
 */
@RequiredArgsConstructor
public class EditContactoService implements EditContactoUseCase {

    private final FindContactoByIdPort findPort;
    private final SaveContactoPort savePort;
    private final CrmAuthorization authorization;

    @Override
    public Contacto edit(EditContactoCommand command) {
        authorization.require(RecursoCrm.CONTACTO, AccionCrm.ACTUALIZAR);
        authorization.requireRecord(RecursoCrm.CONTACTO, AccionCrm.ACTUALIZAR, command.id());
        ContactoId contactoId = ContactoId.from(command.id());

        Contacto existing = findPort.findById(contactoId)
                .orElseThrow(() -> ContactoNotFoundException.forId(command.id()));
        authorization.requireRecord(RecursoCrm.EMPRESA, AccionCrm.LEER, existing.getEmpresaId().value());

        EnumSet<GrupoCampoSensible> changedPrivateGroups = EnumSet.noneOf(GrupoCampoSensible.class);
        if (command.correo() != null && !Objects.equals(command.correo(), existing.getCorreo())) {
            changedPrivateGroups.add(GrupoCampoSensible.CONTACTO_PRIVADO);
        }
        if (command.telefono() != null && !Objects.equals(command.telefono(), existing.getTelefono())) {
            changedPrivateGroups.add(GrupoCampoSensible.CONTACTO_PRIVADO);
        }
        if (!changedPrivateGroups.isEmpty()) {
            authorization.requireWritableGroups(RecursoCrm.CONTACTO, changedPrivateGroups);
        }

        Contacto updated = Contacto.reconstitute(
                existing.getId(),
                existing.getEmpresaId(),
                command.responsableId() != null ? UsuarioId.from(command.responsableId()) : existing.getResponsableId(),
                existing.getCreadoPor(),
                command.nombre(),
                command.correo() != null ? command.correo() : existing.getCorreo(),
                command.telefono() != null ? command.telefono() : existing.getTelefono(),
                command.cargo() != null ? command.cargo() : existing.getCargo(),
                command.comoNosConocio() != null ? command.comoNosConocio() : existing.getComoNosConocio(),
                existing.getCreadoEn(),
                LocalDateTime.now(),
                command.estadoRelacion() != null ? command.estadoRelacion() : existing.getEstadoRelacion()
        );

        authorization.requireCandidate(RecursoCrm.CONTACTO, AccionCrm.ACTUALIZAR,
                new ResourceScopeCandidate(null, null,
                        updated.getResponsableId() == null ? null : updated.getResponsableId().value(),
                        updated.getCreadoPor() == null ? null : updated.getCreadoPor().value()));
        return savePort.save(updated);
    }
}
