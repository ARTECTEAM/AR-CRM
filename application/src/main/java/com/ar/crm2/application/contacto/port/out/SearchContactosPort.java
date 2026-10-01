package com.ar.crm2.application.contacto.port.out;

import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.UsuarioId;

import java.util.List;

/**
 * One dynamic search capability for Contactos, scoped to a trusted
 * actor.
 *
 * <p>The {@code actorUsuarioId} is the row scope when the active role
 * uses {@code PROPIOS_O_ASIGNADOS}; it is passed as a Domain
 * {@link UsuarioId}, not a raw {@link java.util.UUID}, so the adapter
 * applies {@code (creado_por = actor OR responsable_id = actor)} in
 * the same database-pushed query. It is null only for an explicitly
 * shared resource scope. Every
 * remaining argument is optional and exposed in Domain or JDK types
 * only; {@code null} means "no constraint on that dimension".
 * {@code includePrivateFields} is derived from the current CONTACTO
 * field policy; when false, email and phone never participate in the
 * free-text predicate.
 * {@code maxResults}, when non-null, is a positive bound the adapter
 * applies as a database-level limit; the Application service does not
 * cap or truncate results. The port deliberately avoids a
 * combinatorial family of {@code FindBy...} methods so that any
 * combination of optional filters can be expressed by a single call.
 */
public interface SearchContactosPort {

    List<Contacto> search(
            UsuarioId actorUsuarioId,
            String search,
            EstadoRelacion estadoRelacion,
            EmpresaId empresaId,
            UsuarioId responsableId,
            String comoNosConocio,
            Integer maxResults,
            boolean includePrivateFields
    );
}
