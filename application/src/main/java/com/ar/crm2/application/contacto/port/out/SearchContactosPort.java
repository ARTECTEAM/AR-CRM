package com.ar.crm2.application.contacto.port.out;

import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.UsuarioId;

import java.util.List;

/**
 * Database-pushed actor and parent-company scoped contact search.
 *
 * <p>{@code actorUsuarioId} is the mandatory CONTACTO row scope. It is
 * null only when the active CONTACTO grant explicitly uses shared scope.
 * {@code empresaScopeActorUsuarioId} is the independent EMPRESA row scope
 * applied through the contact's parent company before the result limit; it
 * is null only when the active EMPRESA grant explicitly uses shared scope.
 * Optional business filters can only narrow these authorization predicates.
 */
public interface SearchContactosPort {

    List<Contacto> search(
            UsuarioId actorUsuarioId,
            UsuarioId empresaScopeActorUsuarioId,
            String search,
            EstadoRelacion estadoRelacion,
            EmpresaId empresaId,
            UsuarioId responsableId,
            String comoNosConocio,
            Integer maxResults,
            boolean includePrivateFields
    );
}