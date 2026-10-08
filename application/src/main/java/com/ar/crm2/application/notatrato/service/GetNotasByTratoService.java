package com.ar.crm2.application.notatrato.service;

import com.ar.crm2.application.notatrato.port.in.GetNotasByTratoUseCase;
import com.ar.crm2.application.notatrato.port.out.FindNotasByTratoPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.trato.exception.TratoNotFoundException;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.TratoId;

import java.util.List;
import java.util.UUID;

public final class GetNotasByTratoService implements GetNotasByTratoUseCase {
    private final CrmAuthorization authorization;
    private final FindTratoByIdPort findTratoPort;
    private final FindNotasByTratoPort findPort;

    public GetNotasByTratoService(CrmAuthorization authorization,
                                  FindTratoByIdPort findTratoPort,
                                  FindNotasByTratoPort findPort) {
        this.authorization = authorization;
        this.findTratoPort = findTratoPort;
        this.findPort = findPort;
    }

    @Override
    public List<com.ar.crm2.model.entity.NotaTrato> getByTrato(UUID tratoId) {
        authorization.requireRecord(RecursoCrm.TRATO, AccionCrm.LEER, tratoId);
        Trato trato = findTratoPort.findById(TratoId.from(tratoId))
                .orElseThrow(() -> TratoNotFoundException.forId(tratoId));
        authorization.requireRecord(RecursoCrm.CONTACTO, AccionCrm.LEER, trato.getContactoId().value());
        return findPort.findByTrato(trato.getId());
    }
}