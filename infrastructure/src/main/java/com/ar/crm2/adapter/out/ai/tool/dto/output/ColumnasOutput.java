package com.ar.crm2.adapter.out.ai.tool.dto.output;

import java.util.List;

/** Bounded projection of a fully materialized list; {@code total} is exact. */
public record ColumnasOutput(List<ColumnaOutput> columnas, int total, boolean truncated) {
    public ColumnasOutput {
        columnas = columnas == null ? List.of() : List.copyOf(columnas);
    }
}
