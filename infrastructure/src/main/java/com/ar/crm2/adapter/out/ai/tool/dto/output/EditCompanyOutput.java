package com.ar.crm2.adapter.out.ai.tool.dto.output;

/**
 * Bounded, model-visible output of the {@code edit_company} tool.
 *
 * <p>Returns the canonical company identity plus the editable business
 * fields the agent needs to refer back to the saved company. Internal
 * fields ({@code creadoPor}, {@code creadoEn}/{@code actualizadoEn},
 * audit/owner identity, social links, {@code notas}) are intentionally
 * stripped.
 *
 * <p>The {@code estadoRelacion} and {@code responsableId} fields are
 * surfaced because the canonical edit use case persists them as part
 * of the editable set; surfacing them is honest about what the use
 * case actually changed.
 */
public record EditCompanyOutput(
        String id,
        String nombre,
        String sector,
        String estadoRelacion,
        String responsableId,
        String paginaWeb,
        String telefono,
        String notas
) {
}
