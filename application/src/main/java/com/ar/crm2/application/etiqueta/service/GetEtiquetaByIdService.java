package com.ar.crm2.application.etiqueta.service;

import com.ar.crm2.application.etiqueta.exception.EtiquetaNotFoundException;
import com.ar.crm2.application.etiqueta.port.in.GetEtiquetaByIdUseCase;
import com.ar.crm2.application.etiqueta.port.out.FindEtiquetaByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.application.etiqueta.command.GetEtiquetaByIdCommand;
import com.ar.crm2.model.entity.Etiqueta;
import com.ar.crm2.model.vo.EtiquetaId;
import lombok.RequiredArgsConstructor;

/**
 * Application service for retrieving a single Etiqueta by id.
 *
 * <p>Note: Transaction boundary is owned by the infrastructure adapter.
 */
@RequiredArgsConstructor
public class GetEtiquetaByIdService implements GetEtiquetaByIdUseCase {

    private final FindEtiquetaByIdPort findPort;
    private final CrmAuthorization authorization;

    @Override
    public Etiqueta getById(GetEtiquetaByIdCommand command) {
        authorization.require(RecursoCrm.ETIQUETA, AccionCrm.LEER);
        authorization.requireRecord(RecursoCrm.ETIQUETA, AccionCrm.LEER, command.id());
        EtiquetaId id = EtiquetaId.from(command.id());
        Etiqueta etiqueta = findPort.findById(id)
                .orElseThrow(() -> EtiquetaNotFoundException.forId(command.id()));
        return etiqueta;
    }
}
