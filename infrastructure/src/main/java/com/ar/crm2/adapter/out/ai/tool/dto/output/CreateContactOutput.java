package com.ar.crm2.adapter.out.ai.tool.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bounded, model-visible output of the {@code create_contact} tool.
 *
 * <p>Returns the canonical contact identity and the two business
 * fields the agent needs to refer back to it. Internal fields
 * ({@code creadoPor}, {@code actualizadoEn}, {@code responsableId},
 * etc.) are stripped.
 */
public record CreateContactOutput(
        @JsonProperty("id") String id,
        @JsonProperty("nombre") String nombre,
        @JsonProperty("estadoRelacion") String estadoRelacion,
        @JsonProperty("correo") String correo
) {
}