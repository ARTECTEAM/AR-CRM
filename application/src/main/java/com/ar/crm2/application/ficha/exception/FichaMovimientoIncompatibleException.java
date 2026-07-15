package com.ar.crm2.application.ficha.exception;

import com.ar.crm2.model.enums.TipoFicha;
import com.ar.crm2.model.enums.TipoTablero;

import java.util.UUID;

/**
 * Exception thrown when a Ficha is moved to a column that does not belong to
 * a compatible tablero workflow.
 */
public class FichaMovimientoIncompatibleException extends RuntimeException {

    private FichaMovimientoIncompatibleException(String message) {
        super(message);
    }

    public static FichaMovimientoIncompatibleException forTargetColumn(
        UUID fichaId,
        UUID columnaId,
        TipoFicha tipoFicha,
        TipoTablero expectedTipoTablero
    ) {
        return new FichaMovimientoIncompatibleException(
            "La ficha " + fichaId + " de tipo " + tipoFicha
                + " no puede moverse a la columna " + columnaId
                + " porque no pertenece a un tablero " + expectedTipoTablero + "."
        );
    }
}
