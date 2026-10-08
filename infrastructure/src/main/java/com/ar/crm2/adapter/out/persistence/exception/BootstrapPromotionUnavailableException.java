package com.ar.crm2.adapter.out.persistence.exception;

/** Signals that the one-time bootstrap self-promotion is no longer available. */
public final class BootstrapPromotionUnavailableException extends RuntimeException {
    public BootstrapPromotionUnavailableException(String message) {
        super(message);
    }
}
