package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.trato.command.EditTratoCommand;
import com.ar.crm2.application.trato.exception.TratoNotFoundException;
import com.ar.crm2.application.trato.port.in.EditTratoUseCase;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.application.trato.port.out.SaveTratoPort;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * Application service implementing EditTratoUseCase.
 * Orchestrates loading the aggregate, applying the immutable domain update, and saving.
 * Preserves: id, contactoId, creadoEn, estado.
 */
@RequiredArgsConstructor
public class EditTratoService implements EditTratoUseCase {

    private final FindTratoByIdPort findPort;
    private final SaveTratoPort savePort;

    @Override
    public Trato edit(EditTratoCommand command) {
        TratoId tratoId = TratoId.from(command.id());

        Trato existing = findPort.findById(tratoId)
                .orElseThrow(() -> TratoNotFoundException.forId(command.id()));

        Trato updated = Trato.reconstitute(
                existing.getId(),
                existing.getContactoId(),
                command.responsableId() != null ? UsuarioId.from(command.responsableId()) : null,
                command.nombre(),
                command.valorEstimado(),
                command.probabilidad(),
                command.fechaCierreEsperada(),
                command.tipoContrato(),
                existing.getEstado(),
                existing.getCreadoEn(),
                LocalDateTime.now()
        );

        return savePort.save(updated);
    }
}
