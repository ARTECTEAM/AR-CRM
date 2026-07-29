package com.ar.crm2.application.contacto.exception;

import java.util.Objects;

/**
 * Exception thrown when a {@code GetAllContactosCommand} cannot be
 * coordinated into a valid Domain query.
 *
 * <p>Framework-free: it does not depend on Spring, JPA, HTTP, or any
 * container concept. The exception always carries the offending
 * {@link Reason} and the concrete rejected value so callers can
 * diagnose the failure without parsing the message.
 *
 * <p>It extends {@link IllegalArgumentException} so the existing
 * {@code GlobalExceptionHandler.handleIllegalArgument} handler maps the
 * Command-originated rejection to HTTP 400 without any Infrastructure
 * conversion.
 */
public class InvalidGetAllContactosCommandException extends IllegalArgumentException {

    /**
     * Diagnostic categories the Command uses to reject a
     * {@code GetAllContactosCommand} during construction.
     */
    public enum Reason {
        /** {@code actorUsuarioId} is null: security scope cannot be derived. */
        MISSING_ACTOR,
        INVALID_ESTADO_RELACION
    }

    private final Reason reason;
    private final String rejectedValue;

    private InvalidGetAllContactosCommandException(Reason reason, String rejectedValue) {
        super(buildMessage(reason, rejectedValue));
        this.reason = Objects.requireNonNull(reason, "reason");
        this.rejectedValue = rejectedValue;
    }

    public static InvalidGetAllContactosCommandException missingActor() {
        return new InvalidGetAllContactosCommandException(Reason.MISSING_ACTOR, null);
    }

    public static InvalidGetAllContactosCommandException invalidEstadoRelacion(String rejectedValue) {
        return new InvalidGetAllContactosCommandException(Reason.INVALID_ESTADO_RELACION, rejectedValue);
    }

    public Reason getReason() {
        return reason;
    }

    public String getRejectedValue() {
        return rejectedValue;
    }

    private static String buildMessage(Reason reason, String rejectedValue) {
        String value = rejectedValue == null ? "<null>" : "\"" + rejectedValue + "\"";
        return "Invalid GetAllContactosCommand reason=" + reason.name() + " rejectedValue=" + value;
    }
}
