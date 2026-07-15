package com.ar.crm2.application.ficha.service;

import com.ar.crm2.application.ficha.command.MoverColumnaFichaCommand;
import com.ar.crm2.application.ficha.exception.FichaMovimientoIncompatibleException;
import com.ar.crm2.application.ficha.exception.FichaNotFoundException;
import com.ar.crm2.application.ficha.port.in.MoverColumnaFichaUseCase;
import com.ar.crm2.application.ficha.port.out.ExistsColumnaCompatibleConFichaPort;
import com.ar.crm2.application.ficha.port.out.FindFichaByIdPort;
import com.ar.crm2.application.ficha.port.out.SaveFichaPort;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.model.enums.TipoFicha;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.FichaId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MoverColumnaFichaService implements MoverColumnaFichaUseCase {

    private final FindFichaByIdPort findPort;
    private final SaveFichaPort savePort;
    private final ExistsColumnaCompatibleConFichaPort compatibilityPort;

    @Override
    public Ficha moverAColumna(MoverColumnaFichaCommand command) {
        FichaId fichaId = FichaId.from(command.fichaId());
        ColumnaId targetColumnaId = ColumnaId.from(command.targetColumnaId());

        Ficha existing = findPort.findById(fichaId)
                .orElseThrow(() -> FichaNotFoundException.forId(command.fichaId()));

        TipoTablero expectedTipoTablero = expectedTipoTablero(existing.getTipoFicha());
        if (!compatibilityPort.existsCompatibleColumn(targetColumnaId, expectedTipoTablero)) {
            throw FichaMovimientoIncompatibleException.forTargetColumn(
                command.fichaId(),
                command.targetColumnaId(),
                existing.getTipoFicha(),
                expectedTipoTablero
            );
        }

        Ficha updated = existing.moverAColumna(targetColumnaId);

        return savePort.save(updated);
    }

    private static TipoTablero expectedTipoTablero(TipoFicha tipoFicha) {
        return switch (tipoFicha) {
            case TAREA -> TipoTablero.TAREAS;
            case TRATO -> TipoTablero.TRATOS;
        };
    }
}
