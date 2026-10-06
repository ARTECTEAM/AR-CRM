package com.ar.crm2.adapter.out.ai.tool.dto.output;

import java.math.BigDecimal;
import java.util.List;

public record TableroOutput(
        String id,
        String nombre,
        String descripcion,
        String tipoTablero,
        List<ColumnaAssignment> columnas,
        int totalColumnas,
        boolean columnasTruncated) {

    public TableroOutput {
        columnas = columnas == null ? List.of() : List.copyOf(columnas);
    }

    public record ColumnaAssignment(
            String columnaId,
            String tipoTablero,
            Integer limiteWip,
            String nota,
            BigDecimal totalValorEstimado) {
    }
}
