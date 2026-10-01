package com.ar.crm2.application.ficha.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.etiqueta.port.out.FindEtiquetasByIdsPort;
import com.ar.crm2.application.ficha.command.EditFichaCommand;
import com.ar.crm2.application.ficha.exception.FichaNotFoundException;
import com.ar.crm2.application.ficha.port.in.EditFichaUseCase;
import com.ar.crm2.application.ficha.port.out.FindFichaByIdPort;
import com.ar.crm2.application.ficha.port.out.SaveFichaPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.model.entity.FichaEtiqueta;
import com.ar.crm2.model.enums.TipoEtiqueta;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.FichaId;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.TareaId;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Application service implementing EditFichaUseCase.
 *
 * <p>Loads the aggregate, applies the immutable domain update (reconstitute
 * with new fields and etiquetaIds) and saves. The etiqueta relations are
 * resolved through {@link FindEtiquetasByIdsPort}; if a resolved Etiqueta
 * has a mismatched TipoEtiqueta, the call is rejected with
 * {@link com.ar.crm2.exception.EtiquetaTypeMismatchException}.
 */
@RequiredArgsConstructor
public class EditFichaService implements EditFichaUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindFichaByIdPort findPort;
    private final SaveFichaPort savePort;
    private final FindEtiquetasByIdsPort findEtiquetasPort;

    @Override
    public Ficha edit(EditFichaCommand command) {
        FichaId fichaId = FichaId.from(command.id());
        FichaAccessPolicy.requireDestinationCandidate(crmAuthorization, AccionCrm.ACTUALIZAR, command.columnaId());
        crmAuthorization.requireRecord(RecursoCrm.FICHA, AccionCrm.ACTUALIZAR, command.id());

        Ficha existing = findPort.findById(fichaId)
            .orElseThrow(() -> FichaNotFoundException.forId(command.id()));
        FichaAccessPolicy.requireLinkedRecords(crmAuthorization, existing);

        Ficha updated = Ficha.reconstitute(
            existing.getId(),
            ColumnaId.from(command.columnaId()),
            command.tipoFicha(),
            command.tratoId() != null ? TratoId.from(command.tratoId()) : null,
            command.tareaId() != null ? TareaId.from(command.tareaId()) : null,
            Instant.now(),
            List.of()
        );
        FichaAccessPolicy.requireWriteTargets(crmAuthorization, updated, command.etiquetaIds());

        List<FichaEtiqueta> relations = command.etiquetaIds() == null
            ? List.of()
            : FichaEtiquetaResolver.resolve(
                findEtiquetasPort,
                command.etiquetaIds(),
                TipoEtiqueta.fromFicha(command.tipoFicha())
            );

        return savePort.save(updated.withEtiquetas(relations));
    }
}
