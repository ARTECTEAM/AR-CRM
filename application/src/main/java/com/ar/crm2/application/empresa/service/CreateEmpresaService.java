package com.ar.crm2.application.empresa.service;

import com.ar.crm2.application.empresa.command.CreateEmpresaCommand;
import com.ar.crm2.application.empresa.port.out.SaveEmpresaPort;
import com.ar.crm2.application.empresa.port.in.CreateEmpresaUseCase;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Empresa;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Application service implementing CreateEmpresaUseCase.
 * Orchestrates domain entity creation and outbound persistence via SaveEmpresaPort.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class CreateEmpresaService implements CreateEmpresaUseCase {

    private final SaveEmpresaPort savePort;
    private final CrmAuthorization authorization;
    private final CurrentActorPort currentActorPort;

    @Override
    public Empresa create(CreateEmpresaCommand command) {
        authorization.require(RecursoCrm.EMPRESA, AccionCrm.CREAR);
        CurrentActor actor = currentActorPort.currentActor()
                .orElseThrow(() -> new CrmActorUnavailableException("Authenticated CRM user is not active"));
        if (command.telefono() != null || command.notas() != null) {
            authorization.requireWritableGroups(RecursoCrm.EMPRESA, Set.of(GrupoCampoSensible.CONTACTO_PRIVADO));
        }
        authorization.requireCandidate(RecursoCrm.EMPRESA, AccionCrm.CREAR,
                new ResourceScopeCandidate(null, null, command.responsableId(), actor.usuarioId()));
        Empresa empresa = Empresa.create(
            command.nombre(),
            command.sector(),
            command.telefono(),
            command.paginaWeb(),
            command.facebook(),
            command.instagram(),
            command.twitter(),
            command.estadoRelacion(),
            command.responsableId() != null ? UsuarioId.from(command.responsableId()) : null,
            UsuarioId.from(actor.usuarioId()),
            command.notas()
        );
        return savePort.save(empresa);
    }
}
