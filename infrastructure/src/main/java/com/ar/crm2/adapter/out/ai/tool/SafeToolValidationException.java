package com.ar.crm2.adapter.out.ai.tool;

/** Explicit allowlist marker for validation errors that may return a stable safe code to the model. */
public final class SafeToolValidationException extends IllegalArgumentException {

    public SafeToolValidationException(String message) {
        super(message);
    }
}
