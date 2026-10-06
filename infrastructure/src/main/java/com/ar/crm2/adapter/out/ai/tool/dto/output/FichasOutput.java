package com.ar.crm2.adapter.out.ai.tool.dto.output;

import java.util.List;

/** Bounded projection of a fully materialized list; {@code total} is exact. */
public record FichasOutput(List<FichaOutput> fichas, int total, boolean truncated) {
    public FichasOutput {
        fichas = fichas == null ? List.of() : List.copyOf(fichas);
    }
}
