package com.ar.crm2.application.agent.command;

/** Reusable structural assertions for agent application commands. */
public final class ApplicationAssert {

    private ApplicationAssert() {
    }

    public static String requiredTrimmed(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public static <T> T required(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    public static void positive(int value, String field) {
        if (value < 1) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }
}
