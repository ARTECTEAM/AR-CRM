package com.ar.crm2.adapter.out.ai.tool.dto.output;

import java.math.BigDecimal;

/**
 * Bounded, model-visible output of the {@code edit_trato} tool.
 *
 * <p>Returns the canonical deal identity plus the editable business
 * fields the agent needs to refer back to the saved deal. Non-editable
 * deal state is preserved by the underlying canonical edit use case.
 *
 * <p>Internal fields (creator/owner/audit identity, internal handles,
 * persistence timestamps, raw SQL, stack traces, JWTs, credentials,
 * cross-owner data) are intentionally stripped.
 *
 * <p>The expected close date is projected as an ISO-8601 string so the
 * bounded contract does not depend on date-module configuration in the
 * framework result converter.
 */
public record EditTratoOutput(
        String id,
        String nombre,
        String responsableId,
        BigDecimal valorEstimado,
        Integer probabilidad,
        String fechaCierreEsperada,
        String tipoContrato
) {
}
