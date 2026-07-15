package com.ar.crm2.model.agent.policy;

public record MemorySafetyContext(
        boolean explicitUserRequest,
        boolean inferredContent,
        boolean containsSecret,
        boolean containsCredential,
        boolean containsAuthenticationMaterial,
        boolean containsRawToolPayload
) {
    public static MemorySafetyContext explicitSafe() {
        return new MemorySafetyContext(true, false, false, false, false, false);
    }
    public static MemorySafetyContext inferred() {
        return new MemorySafetyContext(false, true, false, false, false, false);
    }
    public static MemorySafetyContext secret() {
        return new MemorySafetyContext(true, false, true, false, false, false);
    }
    public static MemorySafetyContext credential() {
        return new MemorySafetyContext(true, false, false, true, false, false);
    }
    public static MemorySafetyContext authenticationMaterial() {
        return new MemorySafetyContext(true, false, false, false, true, false);
    }
    public static MemorySafetyContext rawToolPayload() {
        return new MemorySafetyContext(true, false, false, false, false, true);
    }
}
