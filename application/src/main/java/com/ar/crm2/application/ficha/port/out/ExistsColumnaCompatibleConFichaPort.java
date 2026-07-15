package com.ar.crm2.application.ficha.port.out;

import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.ColumnaId;

/**
 * Checks whether a target column belongs to at least one tablero of the
 * expected workflow type for a ficha movement.
 */
public interface ExistsColumnaCompatibleConFichaPort {

    boolean existsCompatibleColumn(ColumnaId columnaId, TipoTablero expectedTipoTablero);
}
