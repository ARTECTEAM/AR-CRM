package com.ar.crm2.adapter.out.ai.tool.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;

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
 * <p>The expected close date is serialized as an ISO-8601 string to
 * keep the tool's mapper dependency-free; Jackson 3 / JSR-310 is not
 * registered on the shared {@code ObjectMapper} in this slice.
 */
public record EditTratoOutput(
        @JsonProperty("id") String id,
        @JsonProperty("nombre") String nombre,
        @JsonProperty("responsableId") String responsableId,
        @JsonProperty("valorEstimado") BigDecimal valorEstimado,
        @JsonProperty("probabilidad") Integer probabilidad,
        @JsonProperty("fechaCierreEsperada") String fechaCierreEsperada,
        @JsonProperty("tipoContrato") String tipoContrato
) {
}
