package com.ar.crm2.adapter.out.ai.tool.dto.output;

/**
 * Bounded, model-visible output of the {@code create_company} tool.
 *
 * <p>Returns the canonical company identity plus the editable business
 * fields the agent needs to refer back to it. Internal fields
 * ({@code creadoPor}, {@code creadoEn}, audit/owner identity, social
 * links) are intentionally stripped.
 */
public record CreateCompanyOutput(
        String id,
        String nombre,
        String sector,
        String estadoRelacion,
        String responsableId
) {
}
