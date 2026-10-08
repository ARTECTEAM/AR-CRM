package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.trato.command.GetTratoByIdCommand;
import com.ar.crm2.application.trato.exception.TratoNotFoundException;
import com.ar.crm2.application.trato.port.in.GetTratoByIdUseCase;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.TratoId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing GetTratoByIdUseCase.
 * Loads a Trato by id or throws TratoNotFoundException.
 */
@RequiredArgsConstructor
public class GetTratoByIdService implements GetTratoByIdUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindTratoByIdPort findPort;

    @Override
    public Trato getById(GetTratoByIdCommand command) {
        TratoId tratoId = TratoId.from(command.id());
        crmAuthorization.requireRecord(RecursoCrm.TRATO, AccionCrm.LEER, command.id());

        var trato = findPort.findById(tratoId)
            .orElseThrow(() -> TratoNotFoundException.forId(command.id()));
        if (!crmAuthorization.permitsRecord(RecursoCrm.CONTACTO, AccionCrm.LEER,
                trato.getContactoId().value())) {
            throw TratoNotFoundException.forId(command.id());
        }
        return trato;
    }
}
