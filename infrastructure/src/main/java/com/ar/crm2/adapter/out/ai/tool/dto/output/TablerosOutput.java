package com.ar.crm2.adapter.out.ai.tool.dto.output;

import java.util.List;

/** Bounded projection of a fully materialized list; {@code total} is exact. */
public record TablerosOutput(List<TableroOutput> tableros, int total, boolean truncated) {
    public TablerosOutput {
        tableros = tableros == null ? List.of() : List.copyOf(tableros);
    }
}
