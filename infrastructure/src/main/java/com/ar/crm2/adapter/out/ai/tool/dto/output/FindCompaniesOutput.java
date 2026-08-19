package com.ar.crm2.adapter.out.ai.tool.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Bounded, model-visible output of the {@code find_companies} tool.
 *
 * <p>Exposes only the minimum business fields the agent needs to
 * identify and refer back to a company. Internal fields such as
 * {@code creadoPor}, {@code actualizadoEn}, {@code creadoEn},
 * {@code telefono}, {@code paginaWeb}, social links, and {@code notas}
 * are stripped at the mapper boundary so the model never sees them —
 * they are not part of the contract the agent advertises.
 */
public record FindCompaniesOutput(
        @JsonProperty("companies") List<CompanySummary> companies
) {

    /**
     * Per-company bounded summary. {@code id} is the canonical company
     * UUID string; {@code nombre}, {@code sector},
     * {@code estadoRelacion}, and {@code responsableId} are the only
     * business fields exposed. The tool intentionally omits internal
     * handles and audit fields.
     */
    public record CompanySummary(
            @JsonProperty("id") String id,
            @JsonProperty("nombre") String nombre,
            @JsonProperty("sector") String sector,
            @JsonProperty("estadoRelacion") String estadoRelacion,
            @JsonProperty("responsableId") String responsableId
    ) {
    }
}