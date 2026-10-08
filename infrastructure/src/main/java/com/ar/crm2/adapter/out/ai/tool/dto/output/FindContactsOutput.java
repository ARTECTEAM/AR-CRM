package com.ar.crm2.adapter.out.ai.tool.dto.output;

import java.util.List;

/**
 * Bounded, model-visible output of the {@code find_contacts} tool.
 *
 * <p>Exposes only minimal business fields. Internal fields like
 * {@code creadoPor}, {@code actualizadoEn}, {@code responsableId},
 * {@code telefono}, and {@code comoNosConocio} are stripped at the
 * mapper boundary so the model never sees them — they are not part of
 * the contract the agent advertises.
 * The backing query fetches at most one sentinel row beyond the 20-item
 * output cap, so this contract truthfully exposes {@code returned} and
 * {@code truncated}; it does not claim an exact total match count.
 */
public record FindContactsOutput(
        List<ContactSummary> contacts,
        int returned,
        boolean truncated
) {

    public FindContactsOutput {
        contacts = contacts == null ? List.of() : List.copyOf(contacts);
    }

    /**
     * Per-contact bounded summary. {@code id} is the canonical contact
     * UUID string; {@code nombre}, {@code estadoRelacion}, and
     * {@code correo} are the only business fields exposed.
     */
    public record ContactSummary(
            String id,
            String nombre,
            String estadoRelacion,
            String correo
    ) {
    }
}
