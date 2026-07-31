package com.ar.crm2.adapter.out.ai.tool.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Bounded, model-visible output of the {@code find_contacts} tool.
 *
 * <p>Exposes only minimal business fields. Internal fields like
 * {@code creadoPor}, {@code actualizadoEn}, {@code responsableId},
 * {@code telefono}, and {@code comoNosConocio} are stripped at the
 * mapper boundary so the model never sees them — they are not part of
 * the contract the agent advertises.
 */
public record FindContactsOutput(
        @JsonProperty("contacts") List<ContactSummary> contacts
) {

    /**
     * Per-contact bounded summary. {@code id} is the canonical contact
     * UUID string; {@code nombre}, {@code estadoRelacion}, and
     * {@code correo} are the only business fields exposed.
     */
    public record ContactSummary(
            @JsonProperty("id") String id,
            @JsonProperty("nombre") String nombre,
            @JsonProperty("estadoRelacion") String estadoRelacion,
            @JsonProperty("correo") String correo
    ) {
    }
}