package com.ar.crm2.application.security.exception;

public final class CrmAuthorizationDeniedException extends RuntimeException {
    public CrmAuthorizationDeniedException(String message) {
        super(message);
    }
}
