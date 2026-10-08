package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.notatrato.port.out.SaveNotaTratoPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.trato.exception.TratoNotFoundException;
import com.ar.crm2.application.trato.port.in.CambiarEstadoTratoUseCase;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.application.trato.port.out.SaveTratoPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.NotaTrato;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.TratoId;

import java.util.UUID;

public final class CambiarEstadoTratoService implements CambiarEstadoTratoUseCase {
    private final CrmAuthorization authorization;
    private final FindTratoByIdPort findPort;
    private final SaveTratoPort savePort;
    private final SaveNotaTratoPort notaPort;

    public CambiarEstadoTratoService(CrmAuthorization authorization,
                                     FindTratoByIdPort findPort,
                                     SaveTratoPort savePort,
                                     SaveNotaTratoPort notaPort) {
        this.authorization = authorization;
        this.findPort = findPort;
        this.savePort = savePort;
        this.notaPort = notaPort;
    }

    @Override
    public Trato ganar(UUID id) {
        Trato existing = cargar(id, AccionCrm.ACTUALIZAR);
        authorization.requireRecord(RecursoCrm.CONTACTO, AccionCrm.LEER, existing.getContactoId().value());
        Trato trato = savePort.save(existing.ganar());
        notaPort.save(NotaTrato.crearEvento(trato.getId(), "Oportunidad marcada como GANADA"));
        return trato;
    }

    @Override
    public Trato perder(UUID id, String motivo) {
        Trato existing = cargar(id, AccionCrm.ACTUALIZAR);
        authorization.requireRecord(RecursoCrm.CONTACTO, AccionCrm.LEER, existing.getContactoId().value());
        Trato trato = savePort.save(existing.perder(motivo));
        notaPort.save(NotaTrato.crearEvento(trato.getId(), "Oportunidad marcada como PERDIDA: " + motivo));
        return trato;
    }

    private Trato cargar(UUID id, AccionCrm accion) {
        authorization.requireRecord(RecursoCrm.TRATO, accion, id);
        return findPort.findById(TratoId.from(id))
                .orElseThrow(() -> TratoNotFoundException.forId(id));
    }
}