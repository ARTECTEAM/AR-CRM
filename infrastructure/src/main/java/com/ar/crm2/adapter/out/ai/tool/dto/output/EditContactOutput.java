package com.ar.crm2.adapter.out.ai.tool.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;

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
        @JsonProperty("id") String id,
        @JsonProperty("nombre") String nombre,
        @JsonProperty("correo") String correo,
        @JsonProperty("estadoRelacion") String estadoRelacion,
        @JsonProperty("responsableId") String responsableId,
        @JsonProperty("telefono") String telefono,
        @JsonProperty("cargo") String cargo,
        @JsonProperty("comoNosConocio") String comoNosConocio
) {
}