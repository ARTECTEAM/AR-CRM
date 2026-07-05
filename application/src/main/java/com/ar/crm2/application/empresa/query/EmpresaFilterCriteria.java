package com.ar.crm2.application.empresa.query;

import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.vo.UsuarioId;

/** Criteria owned by application for listing Empresas. */
public record EmpresaFilterCriteria(
        String search,
        EstadoRelacion estadoRelacion,
        String sector,
        UsuarioId responsableId,
        WebFilter web
) {
    public enum WebFilter { CON_WEB, SIN_WEB }

    public static EmpresaFilterCriteria empty() {
        return new EmpresaFilterCriteria(null, null, null, null, null);
    }
}
