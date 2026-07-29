package com.ar.crm2.application.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationAssertTest {

    @Test
    void requiredTrimmedReturnsTrimmedTextAndRejectsMissingText() {
        assertEquals("agent-a", ApplicationAssert.requiredTrimmed(" agent-a ", "actorSubject"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ApplicationAssert.requiredTrimmed(" \t", "actorSubject"));

        assertEquals("actorSubject is required", error.getMessage());
    }

    @Test
    void requiredReturnsSameReferenceAndRejectsNull() {
        Object reference = new Object();

        assertSame(reference, ApplicationAssert.required(reference, "turnId"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ApplicationAssert.required(null, "turnId"));

        assertEquals("turnId is required", error.getMessage());
    }

    @Test
    void positiveAcceptsPositiveNumbersAndRejectsZero() {
        assertDoesNotThrow(() -> ApplicationAssert.positive(1, "visibleHistoryLimit"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ApplicationAssert.positive(0, "visibleHistoryLimit"));

        assertEquals("visibleHistoryLimit must be positive", error.getMessage());
    }

    @Test
    void positiveRejectsNegativeNumbers() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ApplicationAssert.positive(-3, "visibleHistoryLimit"));

        assertEquals("visibleHistoryLimit must be positive", error.getMessage());
    }

    @Test
    void optionalTrimmedReturnsTrimmedTextForNonBlankInput() {
        assertEquals("agent-a", ApplicationAssert.optionalTrimmed(" agent-a "));
        assertEquals("agent-a", ApplicationAssert.optionalTrimmed("agent-a"));
    }

    @Test
    void optionalTrimmedReturnsNullForNullOrBlankInput() {
        assertNull(ApplicationAssert.optionalTrimmed(null));
        assertNull(ApplicationAssert.optionalTrimmed(""));
        assertNull(ApplicationAssert.optionalTrimmed("   "));
        assertNull(ApplicationAssert.optionalTrimmed("\t\n"));
    }
}
