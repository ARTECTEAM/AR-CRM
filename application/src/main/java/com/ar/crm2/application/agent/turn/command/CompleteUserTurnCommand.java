package com.ar.crm2.application.agent.turn.command;

import com.ar.crm2.application.shared.ApplicationAssert;

import java.util.UUID;

/**
 * Simple trusted inputs supplied by the future agent adapter to complete a user turn.
 *
 * <p>The Command now carries BOTH the owner subject (the
 * existing {@code actorSubject}) AND the trusted CRM {@code actorUsuarioId}:
 * <ul>
 *   <li>{@code actorSubject} is the original owner identity (already used
 *       throughout the conversation stack); it is kept for owner/handle
 *       bookkeeping only.</li>
 *   <li>{@code actorUsuarioId} is the <strong>trusted CRM actor
 *       identity</strong> (the {@code UsuarioId.value()}) required by the
 *       Application layer as the security scope for CRM-side effects
 *       (e.g. {@code find_contacts}). It is distinct from the owner
 *       subject and is never derived from the model, prompt, or tool
 *       arguments — it comes from the JWT/ActorContext at the agent
 *       ingress and is threaded unchanged through chat completion.</li>
 * </ul>
 *
 * <p>Both identity fields are validated for presence during
 * construction; the caller cannot omit either.
 */
public record CompleteUserTurnCommand(
        String actorSubject,
        UUID actorUsuarioId,
        UUID turnId,
        String opaqueHandle,
        String prompt,
        int visibleHistoryLimit
) {

    public CompleteUserTurnCommand {
        actorSubject = ApplicationAssert.requiredTrimmed(actorSubject, "actorSubject");
        actorUsuarioId = ApplicationAssert.required(actorUsuarioId, "actorUsuarioId");
        turnId = ApplicationAssert.required(turnId, "turnId");
        opaqueHandle = ApplicationAssert.requiredTrimmed(opaqueHandle, "opaqueHandle");
        prompt = ApplicationAssert.requiredTrimmed(prompt, "prompt");
        ApplicationAssert.positive(visibleHistoryLimit, "visibleHistoryLimit");
    }
}
