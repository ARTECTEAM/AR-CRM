package com.ar.crm2.model.agent.enums;

import com.ar.crm2.exception.InvariantViolationException;
import com.ar.crm2.shared.DomainAssert;

/**
 * Speaker provenance of a visible history entry.
 *
 * <p>These roles are <strong>prompt metadata only</strong> used to reconstruct
 * the conversation for the model. They are NOT authorization roles and must
 * never influence permission decisions, owner isolation, or tool allowlists.
 *
 * <p>The set is intentionally closed: any value not in this enum is rejected,
 * preventing accidental introduction of new speaker roles without a deliberate
 * domain decision.
 */
public enum VisibleMessageRole {

    USER,
    ASSISTANT;

    /**
     * Reconstructs the role from the storage form already trimmed to one of
     * the legal upper-case enum names.
     *
     * @param storageName trimmed storage string previously produced by {@link #name()}
     * @return the matching enum constant
     * @throws com.ar.crm2.exception.InvariantViolationException when the
     *         storage form is null, blank, or unknown
     */
    public static VisibleMessageRole fromStorage(String storageName) {
        DomainAssert.notBlank(storageName, "visibleMessageRoleStorage");
        for (VisibleMessageRole candidate : values()) {
            if (candidate.name().equals(storageName)) {
                return candidate;
            }
        }
        throw new InvariantViolationException(
                "El rol de mensaje visible '" + storageName + "' no está permitido.");
    }
}
