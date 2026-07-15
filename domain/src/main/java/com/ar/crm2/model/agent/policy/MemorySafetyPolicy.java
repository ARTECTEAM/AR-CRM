package com.ar.crm2.model.agent.policy;

import com.ar.crm2.exception.MemorySafetyViolationException;
import com.ar.crm2.shared.DomainAssert;

public final class MemorySafetyPolicy {
    private MemorySafetyPolicy() {
    }
    public static void requireEligible(MemorySafetyContext context) {
        DomainAssert.notNull(context, "memorySafetyContext");
        if (!context.explicitUserRequest()) {
            throw MemorySafetyViolationException.explicitRequestRequired();
        }
        if (context.inferredContent() || context.containsSecret() || context.containsCredential()
                || context.containsAuthenticationMaterial() || context.containsRawToolPayload()) {
            throw MemorySafetyViolationException.sensitiveOrInferredContent();
        }
    }
}
