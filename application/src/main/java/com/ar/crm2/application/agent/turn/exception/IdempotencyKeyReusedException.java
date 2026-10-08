package com.ar.crm2.application.agent.turn.exception;

/** Signals that an idempotency key was reused with a different prompt fingerprint. */
public final class IdempotencyKeyReusedException extends RuntimeException {
}
