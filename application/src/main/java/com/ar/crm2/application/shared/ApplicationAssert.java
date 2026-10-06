package com.ar.crm2.application.shared;

/**
 * Reusable structural assertions shared by Application commands and
 * cross-context command builders. This class is intentionally narrow:
 * it performs only structural input checks (presence, positive integer,
 * optional text normalization) and never encodes business rules, Domain
 * rules, port invocations, or layer-specific logic. It is not a general
 * dumping ground for application utilities.
 */
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

    /**
     * Structural text normalization for optional Application inputs.
     * Returns {@code null} for {@code null} or blank input; otherwise
     * returns the input trimmed. This helper never throws and never
     * performs business or Domain validation; it only normalizes
     * whitespace so downstream layers can treat the value consistently.
     */
    public static String optionalTrimmed(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
