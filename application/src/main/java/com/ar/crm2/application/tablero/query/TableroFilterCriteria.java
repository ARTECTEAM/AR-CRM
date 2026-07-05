package com.ar.crm2.application.tablero.query;

import com.ar.crm2.model.enums.TipoTablero;

/** Criteria owned by application for listing Tableros. */
public record TableroFilterCriteria(TipoTablero tipoTablero) {
    public static TableroFilterCriteria empty() {
        return new TableroFilterCriteria(null);
    }
}
