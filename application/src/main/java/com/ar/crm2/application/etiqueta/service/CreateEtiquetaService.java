package com.ar.crm2.application.etiqueta.service;

import com.ar.crm2.application.etiqueta.command.CreateEtiquetaCommand;
import com.ar.crm2.application.etiqueta.port.in.CreateEtiquetaUseCase;
import com.ar.crm2.application.etiqueta.port.out.ExistsEtiquetaByNombreAndTipoPort;
import com.ar.crm2.application.etiqueta.port.out.SaveEtiquetaPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.exception.DuplicateEtiquetaNameException;
import com.ar.crm2.model.entity.Etiqueta;
import lombok.RequiredArgsConstructor;

/**
 * Application service for creating a new Etiqueta in the global catalog.
 * Enforces (nombre, tipoEtiqueta) uniqueness before persisting.
 *
 * <p>Note: Transaction boundary is owned by the infrastructure adapter
 * (caller wraps the call) — the application module is Spring-free by design.
 */
@RequiredArgsConstructor
public class CreateEtiquetaService implements CreateEtiquetaUseCase {

    private final SaveEtiquetaPort savePort;
    private final ExistsEtiquetaByNombreAndTipoPort existsPort;
    private final CrmAuthorization authorization;

    @Override
    public Etiqueta create(CreateEtiquetaCommand command) {
        authorization.require(RecursoCrm.ETIQUETA, AccionCrm.CREAR);
        if (existsPort.exists(command.nombre(), command.tipoEtiqueta(), null)) {
            throw new DuplicateEtiquetaNameException();
        }
        Etiqueta etiqueta = Etiqueta.create(
            command.nombre(),
            command.tipoEtiqueta(),
            command.color()
        );
        return savePort.save(etiqueta);
    }
}
