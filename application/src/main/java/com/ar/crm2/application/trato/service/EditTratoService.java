package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;

import com.ar.crm2.application.trato.command.EditTratoCommand;
import com.ar.crm2.application.trato.exception.TratoNotFoundException;
import com.ar.crm2.application.trato.port.in.EditTratoUseCase;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.application.trato.port.out.SaveTratoPort;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Application service implementing EditTratoUseCase.
 * Orchestrates loading the aggregate, applying the immutable domain update, and saving.
 * Preserves: id, contactoId, creadoEn, estado.
 */
@RequiredArgsConstructor
public class EditTratoService implements EditTratoUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindTratoByIdPort findPort;
    private final SaveTratoPort savePort;

    @Override
    public Trato edit(EditTratoCommand command) {
        TratoId tratoId = TratoId.from(command.id());
        crmAuthorization.requireRecord(RecursoCrm.TRATO, AccionCrm.ACTUALIZAR, command.id());
        if (command.valorEstimado() != null || command.probabilidad() != null
            || command.fechaCierreEsperada() != null) {
            crmAuthorization.requireWritableGroups(RecursoCrm.TRATO, Set.of(GrupoCampoSensible.FINANCIERO));
        }
        if (command.responsableId() != null) {
            crmAuthorization.requireCandidate(RecursoCrm.TRATO, AccionCrm.ACTUALIZAR,
                new ResourceScopeCandidate(null, null, command.responsableId(), null));
        }

        Trato existing = findPort.findById(tratoId)
                .orElseThrow(() -> TratoNotFoundException.forId(command.id()));

        if (!crmAuthorization.permitsRecord(RecursoCrm.CONTACTO, AccionCrm.LEER,
                existing.getContactoId().value())) {
            throw TratoNotFoundException.forId(command.id());
        }
        var resultingResponsibleId = command.responsableId() != null
            ? command.responsableId()
            : existing.getResponsableId().value();
        if (command.responsableId() == null) {
            crmAuthorization.requireCandidate(RecursoCrm.TRATO, AccionCrm.ACTUALIZAR,
                new ResourceScopeCandidate(null, null, resultingResponsibleId, null));
        }

        Trato updated = Trato.reconstitute(
                existing.getId(),
                existing.getContactoId(),
                UsuarioId.from(resultingResponsibleId),
                command.nombre(),
                command.valorEstimado() != null ? command.valorEstimado() : existing.getValorEstimado(),
                command.probabilidad() != null ? command.probabilidad() : existing.getProbabilidad(),
                command.fechaCierreEsperada() != null ? command.fechaCierreEsperada() : existing.getFechaCierreEsperada(),
                command.tipoContrato(),
                existing.getEstado(),
                existing.getCreadoEn(),
                LocalDateTime.now()
        );

        return savePort.save(updated);
    }
}
