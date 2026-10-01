package com.ar.crm2.application.ficha.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.ficha.command.MoverColumnaFichaCommand;
import com.ar.crm2.application.ficha.exception.FichaNotFoundException;
import com.ar.crm2.application.ficha.port.in.MoverColumnaFichaUseCase;
import com.ar.crm2.application.ficha.port.out.FindFichaByIdPort;
import com.ar.crm2.application.ficha.port.out.SaveFichaPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.FichaId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MoverColumnaFichaService implements MoverColumnaFichaUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindFichaByIdPort findPort;
    private final SaveFichaPort savePort;

    @Override
    public Ficha moverAColumna(MoverColumnaFichaCommand command) {
        FichaId fichaId = FichaId.from(command.fichaId());
        ColumnaId targetColumnaId = ColumnaId.from(command.targetColumnaId());
        FichaAccessPolicy.requireDestinationCandidate(crmAuthorization, AccionCrm.ACTUALIZAR,
            command.targetColumnaId());
        crmAuthorization.requireRecord(RecursoCrm.FICHA, AccionCrm.ACTUALIZAR, command.fichaId());
        crmAuthorization.requireRecord(RecursoCrm.COLUMNA, AccionCrm.LEER, command.targetColumnaId());

        Ficha existing = findPort.findById(fichaId)
                .orElseThrow(() -> FichaNotFoundException.forId(command.fichaId()));
        FichaAccessPolicy.requireLinkedRecords(crmAuthorization, existing);

        Ficha updated = existing.moverAColumna(targetColumnaId);

        return savePort.save(updated);
    }
}
