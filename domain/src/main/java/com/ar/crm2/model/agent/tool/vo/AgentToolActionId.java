package com.ar.crm2.model.agent.tool.vo;

import com.ar.crm2.exception.InvariantViolationException;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import com.ar.crm2.shared.DomainAssert;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Deterministic, server-derived identifier for an agent tool action. The id
 * is built from the four trusted inputs — owner, turn, canonical storage
 * tool name, and canonical arguments — and is never accepted from the model
 * or from any user payload. Equal inputs produce equal ids, which is what
 * enables convergent retries on the persistence boundary.
 */
public record AgentToolActionId(String value) {

    private static final Pattern CANONICAL_SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public AgentToolActionId {
        DomainAssert.notBlank(value, "agentToolActionId");
        if (!CANONICAL_SHA_256.matcher(value).matches()) {
            throw new InvariantViolationException(
                    "El campo agentToolActionId debe ser un SHA-256 hexadecimal canónico.");
        }
    }

    /**
     * Derive a SHA-256 hex action id from the four trusted inputs. Any
     * change in any of them yields a different id, and identical inputs
     * always yield the same id.
     */
    public static AgentToolActionId derive(
            AgentOwnerId ownerId,
            TurnId turnId,
            AgentToolName toolName,
            String canonicalArguments
    ) {
        DomainAssert.notNull(ownerId, "ownerId");
        DomainAssert.notNull(turnId, "turnId");
        DomainAssert.notNull(toolName, "toolName");
        DomainAssert.notBlank(canonicalArguments, "canonicalArguments");
        String normalizedCanonicalArguments = canonicalArguments.trim();
        String seed = ownerId.value()
                + "|" + turnId.value().toString()
                + "|" + toolName.storageName()
                + "|" + normalizedCanonicalArguments;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
            return new AgentToolActionId(HexFormat.of().formatHex(hashed));
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is mandated by the JRE — its absence would mean a broken JVM.
            throw new IllegalStateException("SHA-256 is unavailable in this JVM", ex);
        }
    }
}
