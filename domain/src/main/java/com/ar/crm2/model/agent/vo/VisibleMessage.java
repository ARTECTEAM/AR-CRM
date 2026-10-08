package com.ar.crm2.model.agent.vo;

import com.ar.crm2.model.agent.enums.VisibleMessageRole;
import com.ar.crm2.shared.DomainAssert;

/**
 * Immutable visible-history entry carrying the speaker provenance and the
 * persisted content as it was prepared or completed.
 *
 * <p>The role encodes <strong>speaker provenance only</strong>. It is used
 * to reconstruct ordered USER/ASSISTANT turns when assembling the model
 * prompt. It is NEVER an authorization or permission role.
 *
 * <p>Instances are created through {@link #of(VisibleMessageRole, String)},
 * {@link #user(String)}, or {@link #assistant(String)} and are considered
 * equal when their role and trimmed content match.
 */
public record VisibleMessage(VisibleMessageRole role, String content) {

    public VisibleMessage {
        DomainAssert.notNull(role, "visibleMessageRole");
        DomainAssert.notBlank(content, "visibleMessageContent");
        content = content.trim();
    }

    public static VisibleMessage of(VisibleMessageRole role, String content) {
        return new VisibleMessage(role, content);
    }

    public static VisibleMessage user(String content) {
        return new VisibleMessage(VisibleMessageRole.USER, content);
    }

    public static VisibleMessage assistant(String content) {
        return new VisibleMessage(VisibleMessageRole.ASSISTANT, content);
    }
}
