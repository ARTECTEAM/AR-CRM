package com.ar.crm2.application.contacto.query;

import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.UsuarioId;

/** Criteria owned by application for listing Contactos. */
public record ContactoFilterCriteria(
        String search,
        EstadoRelacion estadoRelacion,
        EmpresaId empresaId,
        UsuarioId responsableId,
        String comoNosConocio
) {
    public static ContactoFilterCriteria empty() {
        return new ContactoFilterCriteria(null, null, null, null, null);
    }
}
