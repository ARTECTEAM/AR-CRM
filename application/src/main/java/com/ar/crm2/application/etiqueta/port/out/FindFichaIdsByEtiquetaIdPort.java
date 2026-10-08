package com.ar.crm2.application.etiqueta.port.out;

import com.ar.crm2.model.vo.EtiquetaId;

import java.util.List;
import java.util.UUID;

/** Finds affected Ficha rows before a catalog label cascades its relationship links. */
public interface FindFichaIdsByEtiquetaIdPort {
    List<UUID> findFichaIdsByEtiquetaId(EtiquetaId etiquetaId);
}
