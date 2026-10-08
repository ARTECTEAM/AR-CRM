package com.ar.crm2.application.ficha.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.etiqueta.port.out.FindEtiquetasByIdsPort;
import com.ar.crm2.application.ficha.command.CreateFichaCommand;
import com.ar.crm2.application.ficha.port.in.CreateFichaUseCase;
import com.ar.crm2.application.ficha.port.out.SaveFichaPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.model.entity.FichaEtiqueta;
import com.ar.crm2.model.enums.TipoEtiqueta;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.TareaId;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service implementing CreateFichaUseCase.
 *
 * <p>Resolves the optional etiquetaIds into domain Etiqueta entities through
 * {@link FindEtiquetasByIdsPort}, validates that every Etiqueta type matches
 * the Ficha's TipoFicha, and attaches the resulting FichaEtiqueta relations
 * via {@link Ficha#withEtiquetas(List)}.
 */
@RequiredArgsConstructor
public class CreateFichaService implements CreateFichaUseCase {

    private final CrmAuthorization crmAuthorization;

    private final SaveFichaPort savePort;
    private final FindEtiquetasByIdsPort findEtiquetasPort;

    @Override
    public Ficha create(CreateFichaCommand command) {
        crmAuthorization.require(RecursoCrm.FICHA, AccionCrm.CREAR);
        FichaAccessPolicy.requireDestinationCandidate(crmAuthorization, AccionCrm.CREAR, command.columnaId());
        Ficha ficha = Ficha.create(
            ColumnaId.from(command.columnaId()),
            command.tipoFicha(),
            command.tratoId() != null ? TratoId.from(command.tratoId()) : null,
            command.tareaId() != null ? TareaId.from(command.tareaId()) : null
        );
        FichaAccessPolicy.requireWriteTargets(crmAuthorization, ficha, command.etiquetaIds());

        if (command.etiquetaIds() != null && !command.etiquetaIds().isEmpty()) {
            List<FichaEtiqueta> relations = FichaEtiquetaResolver.resolve(
                findEtiquetasPort,
                command.etiquetaIds(),
                TipoEtiqueta.fromFicha(command.tipoFicha())
            );
            ficha = ficha.withEtiquetas(relations);
        }

        return savePort.save(ficha);
    }
}
