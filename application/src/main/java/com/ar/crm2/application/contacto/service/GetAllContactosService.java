package com.ar.crm2.application.contacto.service;

import com.ar.crm2.application.contacto.command.GetAllContactosCommand;
import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.application.contacto.port.out.SearchContactosPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Application service implementing {@link GetAllContactosUseCase}.
 *
 * <p>Structural validation belongs to the {@link GetAllContactosCommand}:
 * by the time a Command reaches this Service, its {@code estadoRelacion}
 * is either {@code null} or an exact
 * {@link EstadoRelacion} {@code name()}, and its optional filters are
 * already normalized.
 *
 * <p>The Service derives the actor from the authenticated local CRM
 * identity, applies the active role's row and private-search policy,
 * and filters every returned row through the authorization boundary.
 * Command actor IDs are never used as authority.
 */
@RequiredArgsConstructor
public class GetAllContactosService implements GetAllContactosUseCase {

    private final SearchContactosPort searchPort;
    private final CrmAuthorization authorization;
    private final CurrentActorPort currentActorPort;

    @Override
    public List<Contacto> getAll(GetAllContactosCommand command) {
        authorization.require(RecursoCrm.CONTACTO, AccionCrm.LEER);
        CurrentActor actor = currentActorPort.currentActor()
                .orElseThrow(() -> new CrmActorUnavailableException("Authenticated CRM user is not active"));
        UUID actorUsuarioId = actor.usuarioId();
        ResourceReadPolicy readPolicy = authorization.readPolicy(RecursoCrm.CONTACTO);
        UsuarioId queryActor = readPolicy.scope() == AlcanceCrm.PROPIOS_O_ASIGNADOS
                ? UsuarioId.from(actorUsuarioId)
                : null;
        EstadoRelacion estadoRelacion = command.estadoRelacion() == null
                ? null
                : EstadoRelacion.valueOf(command.estadoRelacion());
        EmpresaId empresaId = command.empresaId() == null
                ? null
                : EmpresaId.from(command.empresaId());
        UsuarioId responsableId = command.responsableId() == null
                ? null
                : UsuarioId.from(command.responsableId());

        if (empresaId != null) {
            authorization.requireRecord(RecursoCrm.EMPRESA, AccionCrm.LEER, empresaId.value());
        }

        boolean includePrivateFields = readPolicy.readableGroups().contains(GrupoCampoSensible.CONTACTO_PRIVADO);

        return searchPort.search(
                queryActor,
                command.search(),
                estadoRelacion,
                empresaId,
                responsableId,
                command.comoNosConocio(),
                command.maxResults(),
                includePrivateFields
        ).stream()
                .filter(contacto -> authorization.permitsRecord(
                        RecursoCrm.CONTACTO, AccionCrm.LEER, contacto.getId().value()))
                .filter(contacto -> authorization.permitsRecord(
                        RecursoCrm.EMPRESA, AccionCrm.LEER, contacto.getEmpresaId().value()))
                .toList();
    }
}
