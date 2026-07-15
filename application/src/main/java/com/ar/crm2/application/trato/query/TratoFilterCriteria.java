package com.ar.crm2.application.trato.query;

import com.ar.crm2.model.enums.EstadoTrato;
import com.ar.crm2.model.enums.TipoContrato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.UsuarioId;
import com.ar.crm2.application.shared.query.ListPageRequest;

import java.math.BigDecimal;

/** Criteria owned by application for listing Tratos. */
public record TratoFilterCriteria(
        String search,
        EstadoTrato estado,
        TipoContrato tipoContrato,
        UsuarioId responsableId,
        ContactoId contactoId,
        BigDecimal valorMin,
        BigDecimal valorMax,
        CierreEsperadoFilter cierreEsperado,
        ListPageRequest pageRequest
) {
    public enum CierreEsperadoFilter { TODAS, VENCIDAS, PROXIMOS_7, PROXIMOS_30, SIN_FECHA }

    public TratoFilterCriteria(String search, EstadoTrato estado, TipoContrato tipoContrato, UsuarioId responsableId,
            ContactoId contactoId, BigDecimal valorMin, BigDecimal valorMax, CierreEsperadoFilter cierreEsperado) {
        this(search, estado, tipoContrato, responsableId, contactoId, valorMin, valorMax, cierreEsperado, ListPageRequest.unpaged());
    }

    public static TratoFilterCriteria empty() {
        return new TratoFilterCriteria(null, null, null, null, null, null, null, null, ListPageRequest.unpaged());
    }
}
