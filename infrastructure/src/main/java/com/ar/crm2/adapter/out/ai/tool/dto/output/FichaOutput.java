package com.ar.crm2.adapter.out.ai.tool.dto.output;

import java.util.List;

public record FichaOutput(
        String id,
        String columnaId,
        String tipoFicha,
        String tratoId,
        String tareaId,
        List<String> etiquetaIds,
        int totalEtiquetas,
        boolean etiquetasTruncated) {

    public FichaOutput {
        etiquetaIds = etiquetaIds == null ? List.of() : List.copyOf(etiquetaIds);
    }
}
