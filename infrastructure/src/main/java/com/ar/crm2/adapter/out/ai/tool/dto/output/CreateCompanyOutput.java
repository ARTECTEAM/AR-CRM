package com.ar.crm2.adapter.out.ai.tool.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bounded, model-visible output of the {@code create_company} tool.
 *
 * <p>Returns the canonical company identity plus the editable business
 * fields the agent needs to refer back to it. Internal fields
 * ({@code creadoPor}, {@code creadoEn}, audit/owner identity, social
 * links) are intentionally stripped.
 */
public record CreateCompanyOutput(
        @JsonProperty("id") String id,
        @JsonProperty("nombre") String nombre,
        @JsonProperty("sector") String sector,
        @JsonProperty("estadoRelacion") String estadoRelacion,
        @JsonProperty("responsableId") String responsableId
) {
}