package com.ar.crm2.application.agent.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationAssertTest {

    @Test
    void requiredTrimmedReturnsTrimmedTextAndRejectsMissingTextWithExactMessage() {
        assertEquals("agent-a", ApplicationAssert.requiredTrimmed(" agent-a ", "actorSubject"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ApplicationAssert.requiredTrimmed(" \t", "actorSubject"));

        assertEquals("actorSubject is required", exception.getMessage());
    }

    @Test
    void requiredReturnsSameReferenceAndRejectsNullWithExactMessage() {
        Object reference = new Object();

        assertSame(reference, ApplicationAssert.required(reference, "turnId"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ApplicationAssert.required(null, "turnId"));

        assertEquals("turnId is required", exception.getMessage());
    }

    @Test
    void positiveAcceptsPositiveNumbersAndRejectsZeroWithExactMessage() {
        assertDoesNotThrow(() -> ApplicationAssert.positive(1, "visibleHistoryLimit"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ApplicationAssert.positive(0, "visibleHistoryLimit"));

        assertEquals("visibleHistoryLimit must be positive", exception.getMessage());
    }
}
