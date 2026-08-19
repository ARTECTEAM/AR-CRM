package com.ar.crm2.model.agent.tool.vo;

/**
 * Strict, server-owned allowlist of tool names the conversational agent
 * is permitted to invoke. Model-supplied or client-supplied tool names
 * that are not on this list MUST be rejected by the Application layer
 * before any delegation occurs. The {@link #storageName()} is the
 * canonical string that flows through prompts, persistence, and the
 * canonical-argument fingerprint.
 */
public enum AgentToolName {

    FIND_CONTACTS("find_contacts"),
    CREATE_CONTACT("create_contact"),
    EDIT_CONTACT("edit_contact"),
    CREATE_COMPANY("create_company"),
    EDIT_COMPANY("edit_company"),
    EDIT_TRATO("edit_trato");

    private final String storageName;

    AgentToolName(String storageName) {
        this.storageName = storageName;
    }

    public String storageName() {
        return storageName;
    }

    /**
     * Resolve a model-supplied tool name to the allowlist entry. Returns
     * {@code null} when the supplied name is blank or unknown; the
     * Application layer is responsible for rejecting unrecognised inputs
     * with a controlled domain outcome. Case-sensitive matching is
     * intentional — the allowlist is an explicit contract, not a best
     * effort.
     */
    public static AgentToolName fromStorageName(String storageName) {
        if (storageName == null || storageName.isBlank()) {
            return null;
        }
        for (AgentToolName candidate : values()) {
            if (candidate.storageName.equals(storageName)) {
                return candidate;
            }
        }
        return null;
    }
}
