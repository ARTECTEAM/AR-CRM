package com.ar.crm2.application.security.exception;

public class AuthenticatedUsuarioRequiredException extends RuntimeException {

    public AuthenticatedUsuarioRequiredException(String message) {
        super(message);
    }

    public static AuthenticatedUsuarioRequiredException forMissingUsuarioId() {
        return new AuthenticatedUsuarioRequiredException(
                "No active local CRM user is linked to the authenticated JWT subject");
    }

    public static AuthenticatedUsuarioRequiredException forMissingActorContext() {
        return new AuthenticatedUsuarioRequiredException(
                "authenticated actor context not found — ensure the request is authenticated");
    }
}
