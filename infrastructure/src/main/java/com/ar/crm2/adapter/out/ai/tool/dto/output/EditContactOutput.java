package com.ar.crm2.adapter.out.ai.tool.dto.output;

/**
 * Bounded, model-visible output of the {@code edit_contact} tool.
 *
 * <p>Returns the canonical contact identity plus the editable business
 * fields the agent needs to refer back to the saved contact. Internal
 * fields ({@code creadoPor}, {@code empresaId},
 * {@code creadoEn}/{@code actualizadoEn}, audit/owner identity) are
 * intentionally stripped so they never reach the model.
 *
 * <p>The {@code estadoRelacion} field is included because the canonical
 * edit use case persists it as part of the editable set; surfacing it
 * is honest about what the use case actually changed.
 */
public record EditContactOutput(
        String id,
        String nombre,
        String correo,
        String estadoRelacion,
        String responsableId,
        String telefono,
        String cargo,
        String comoNosConocio
) {
}
