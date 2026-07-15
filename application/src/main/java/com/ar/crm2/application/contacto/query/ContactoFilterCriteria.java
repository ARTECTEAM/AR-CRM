package com.ar.crm2.application.contacto.query;

import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.UsuarioId;
import com.ar.crm2.application.shared.query.ListPageRequest;

/** Criteria owned by application for listing Contactos. */
public record ContactoFilterCriteria(
        String search,
        EstadoRelacion estadoRelacion,
        EmpresaId empresaId,
        UsuarioId responsableId,
        String comoNosConocio,
        ListPageRequest pageRequest
) {
    public ContactoFilterCriteria(String search, EstadoRelacion estadoRelacion, EmpresaId empresaId, UsuarioId responsableId, String comoNosConocio) {
        this(search, estadoRelacion, empresaId, responsableId, comoNosConocio, ListPageRequest.unpaged());
    }

    public static ContactoFilterCriteria empty() {
        return new ContactoFilterCriteria(null, null, null, null, null, ListPageRequest.unpaged());
    }
}
