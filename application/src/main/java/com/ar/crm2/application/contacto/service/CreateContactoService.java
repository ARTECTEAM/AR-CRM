package com.ar.crm2.application.contacto.service;

import com.ar.crm2.application.contacto.command.CreateContactoCommand;
import com.ar.crm2.application.contacto.port.out.SaveContactoPort;
import com.ar.crm2.application.contacto.port.in.CreateContactoUseCase;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Application service implementing CreateContactoUseCase.
 * Orchestrates domain entity creation and outbound persistence via SaveContactoPort.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class CreateContactoService implements CreateContactoUseCase {

    private final SaveContactoPort savePort;
    private final CrmAuthorization authorization;
    private final CurrentActorPort currentActorPort;

    @Override
    public Contacto create(CreateContactoCommand command) {
        authorization.require(RecursoCrm.CONTACTO, AccionCrm.CREAR);
        authorization.requireRecord(RecursoCrm.EMPRESA, AccionCrm.LEER, command.empresaId());
        CurrentActor actor = currentActorPort.currentActor()
                .orElseThrow(() -> new CrmActorUnavailableException("Authenticated CRM user is not active"));
        if (command.correo() != null || command.telefono() != null) {
            authorization.requireWritableGroups(RecursoCrm.CONTACTO, Set.of(GrupoCampoSensible.CONTACTO_PRIVADO));
        }
        authorization.requireCandidate(RecursoCrm.CONTACTO, AccionCrm.CREAR,
                new ResourceScopeCandidate(null, null, command.responsableId(), actor.usuarioId()));
        Contacto contacto = Contacto.create(
            EmpresaId.from(command.empresaId()),
            command.nombre(),
            command.correo(),
            command.estadoRelacion(),
            command.responsableId() != null ? UsuarioId.from(command.responsableId()) : null,
            UsuarioId.from(actor.usuarioId()),
            command.telefono(),
            command.cargo(),
            command.comoNosConocio()
        );
        return savePort.save(contacto);
    }
}
