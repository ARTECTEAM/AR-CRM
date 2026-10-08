package com.ar.crm2.application.etiqueta.service;

import com.ar.crm2.application.etiqueta.port.in.GetAllEtiquetasUseCase;
import com.ar.crm2.application.etiqueta.port.out.FindAllEtiquetasPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Etiqueta;
import com.ar.crm2.model.enums.TipoEtiqueta;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * Application service for listing Etiquetas, optionally filtered by type.
 *
 * <p>Note: Transaction boundary is owned by the infrastructure adapter.
 */
@RequiredArgsConstructor
public class GetAllEtiquetasService implements GetAllEtiquetasUseCase {

    private final FindAllEtiquetasPort findAllPort;
    private final CrmAuthorization authorization;

    @Override
    public List<Etiqueta> getAll(Optional<TipoEtiqueta> tipoEtiqueta) {
        authorization.require(RecursoCrm.ETIQUETA, AccionCrm.LEER);
        return findAllPort.findAll(tipoEtiqueta).stream()
                .filter(etiqueta -> authorization.permitsRecord(
                        RecursoCrm.ETIQUETA, AccionCrm.LEER, etiqueta.getId().value()))
                .toList();
    }
}
