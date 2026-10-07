package com.ar.crm2.application.contacto.service;

import com.ar.crm2.application.contacto.command.GetAllContactosCommand;
import com.ar.crm2.application.contacto.port.in.SearchContactosForActorUseCase;
import com.ar.crm2.application.contacto.port.out.SearchContactosPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service implementing {@link SearchContactosForActorUseCase}.
 *
 * <p>The Service owns Domain conversion only. Structural validation
 * belongs to the {@link GetAllContactosCommand}: by the time a Command
 * reaches this Service, its {@code actorUsuarioId} is non-null, its
 * {@code estadoRelacion} is either {@code null} or an exact
 * {@link EstadoRelacion} {@code name()}, and its optional filters are
 * already normalized.
 *
 * <p>The Service converts the actor and optional filters to Domain
 * types (with explicit null-handling) and delegates to
 * {@link SearchContactosPort}. Contact and parent-company actor scopes are passed separately from the optional {@code responsableId} filter. Each scope is derived from its own resource grant, and the adapter applies both in one query before the database-level result limit.
 */
@RequiredArgsConstructor
public class SearchContactosForActorService implements SearchContactosForActorUseCase {

    private final CrmAuthorization crmAuthorization;
    private final SearchContactosPort searchPort;

    @Override
    public List<Contacto> search(GetAllContactosCommand command) {
        UsuarioId actorUsuarioId = UsuarioId.from(command.actorUsuarioId());
        EstadoRelacion estadoRelacion = command.estadoRelacion() == null
                ? null
                : EstadoRelacion.valueOf(command.estadoRelacion());
        EmpresaId empresaId = command.empresaId() == null
                ? null
                : EmpresaId.from(command.empresaId());
        UsuarioId responsableId = command.responsableId() == null
                ? null
                : UsuarioId.from(command.responsableId());

        var policy = crmAuthorization.readPolicy(RecursoCrm.CONTACTO);
        var empresaPolicy = crmAuthorization.readPolicy(RecursoCrm.EMPRESA);
        UsuarioId scopeActorId = policy.scope() == AlcanceCrm.TODO_COMPARTIDO ? null : actorUsuarioId;
        UsuarioId empresaScopeActorId = empresaPolicy.scope() == AlcanceCrm.TODO_COMPARTIDO ? null : actorUsuarioId;

        return searchPort.search(
                scopeActorId,
                empresaScopeActorId,
                command.search(),
                estadoRelacion,
                empresaId,
                responsableId,
                command.comoNosConocio(),
                command.maxResults(),
                policy.readableGroups()
                        .contains(GrupoCampoSensible.CONTACTO_PRIVADO)
        );
    }
}
