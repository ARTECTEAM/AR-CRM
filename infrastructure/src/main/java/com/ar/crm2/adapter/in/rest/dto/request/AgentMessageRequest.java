package com.ar.crm2.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * REST request DTO for the Pipely CRM conversational ingress.
 *
 * <p>Body carries only the caller-supplied message and the supplied
 * idempotency key. Owner/actor identity is intentionally absent: every
 * authenticated identity MUST be derived from the validated JWT
 * through the existing {@code ActorContextRequestAttributeFilter}.
 * Unknown body fields are ignored by Jackson's default mapper
 * configuration (consistent with the rest of the REST surface).
 *
 * <p>Whitespace-only values are rejected at this boundary because both
 * fields participate in idempotency hashing and persisted-history
 * fingerprinting.
 */
public record AgentMessageRequest(

        @NotBlank(message = "message is required")
        @Size(min = 1, max = 4000, message = "message must be 1-4000 characters")
        String message,

        @NotBlank(message = "idempotencyKey is required")
        @Size(min = 1, max = 200, message = "idempotencyKey must be 1-200 characters")
        String idempotencyKey
) {
}