package com.ar.crm2.adapter.out.ai.tool.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bounded, model-visible output of the {@code update_deal_stage} tool.
 *
 * <p>Returns the deal identity and the resulting deal status. The
 * {@code motivo} is intentionally NOT exposed in the model-facing
 * result — the loss reason is sensitive business detail and the model
 * already received it as a required input argument. Internal fields
 * like {@code contactoId}, {@code responsableId}, {@code valorEstimado},
 * etc. are stripped.
 */
public record UpdateDealStageOutput(
        @JsonProperty("id") String id,
        @JsonProperty("status") String status
) {
}