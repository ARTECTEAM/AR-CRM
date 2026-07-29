package com.ar.crm2.application.contacto.service;

import com.ar.crm2.application.contacto.command.GetAllContactosCommand;
import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.application.contacto.port.out.SearchContactosPort;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service implementing {@link GetAllContactosUseCase}.
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
 * {@link SearchContactosPort}. The actor is passed as the first port
 * parameter, separate from the optional {@code responsableId} filter:
 * actor scope is security, {@code responsableId} is an optional
 * narrowing predicate that can never replace actor scope. The Service
 * does not cap or truncate results; the database-level limit is the
 * adapter's concern.
 */
@RequiredArgsConstructor
public class GetAllContactosService implements GetAllContactosUseCase {

    private final SearchContactosPort searchPort;

    @Override
    public List<Contacto> getAll(GetAllContactosCommand command) {
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

        return searchPort.search(
                actorUsuarioId,
                command.search(),
                estadoRelacion,
                empresaId,
                responsableId,
                command.comoNosConocio(),
                command.maxResults()
        );
    }
}
