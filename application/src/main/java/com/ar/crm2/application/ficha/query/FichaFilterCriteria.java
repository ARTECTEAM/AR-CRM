package com.ar.crm2.application.ficha.query;

import com.ar.crm2.model.enums.TipoFicha;
import com.ar.crm2.model.vo.TareaId;
import com.ar.crm2.model.vo.TratoId;

import java.util.Set;

/** Criteria owned by application for listing Fichas. */
public record FichaFilterCriteria(
        TipoFicha tipoFicha,
        TratoId tratoId,
        TareaId tareaId,
        Set<TratoId> tratoIds,
        Set<TareaId> tareaIds
) {
    public static FichaFilterCriteria empty() {
        return new FichaFilterCriteria(null, null, null, Set.of(), Set.of());
    }
}
