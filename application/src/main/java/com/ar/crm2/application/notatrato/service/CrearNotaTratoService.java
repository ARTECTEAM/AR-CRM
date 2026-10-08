package com.ar.crm2.application.notatrato.service;

import com.ar.crm2.application.notatrato.port.in.CrearNotaTratoUseCase;
import com.ar.crm2.application.notatrato.port.out.SaveNotaTratoPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.application.trato.exception.TratoNotFoundException;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.NotaTrato;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;

import java.util.UUID;

public final class CrearNotaTratoService implements CrearNotaTratoUseCase {
    private final CrmAuthorization authorization;
    private final CurrentActorPort currentActorPort;
    private final FindTratoByIdPort findTratoPort;
    private final SaveNotaTratoPort savePort;

    public CrearNotaTratoService(CrmAuthorization authorization,
                                 CurrentActorPort currentActorPort,
                                 FindTratoByIdPort findTratoPort,
                                 SaveNotaTratoPort savePort) {
        this.authorization = authorization;
        this.currentActorPort = currentActorPort;
        this.findTratoPort = findTratoPort;
        this.savePort = savePort;
    }

    @Override
    public NotaTrato crear(UUID tratoId, UUID ignoredAutorId, String contenido) {
        authorization.requireRecord(RecursoCrm.TRATO, AccionCrm.ACTUALIZAR, tratoId);
        Trato trato = findTratoPort.findById(TratoId.from(tratoId))
                .orElseThrow(() -> TratoNotFoundException.forId(tratoId));
        authorization.requireRecord(RecursoCrm.CONTACTO, AccionCrm.LEER, trato.getContactoId().value());
        var actor = currentActorPort.currentActor()
                .orElseThrow(() -> new CrmActorUnavailableException("No active CRM user is linked to this request"));
        return savePort.save(NotaTrato.crearNota(trato.getId(), UsuarioId.from(actor.usuarioId()), contenido));
    }
}