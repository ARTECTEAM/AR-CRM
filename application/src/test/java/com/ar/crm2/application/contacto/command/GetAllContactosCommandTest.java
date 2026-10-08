package com.ar.crm2.application.contacto.command;

import com.ar.crm2.application.contacto.exception.InvalidGetAllContactosCommandException;
import com.ar.crm2.model.enums.EstadoRelacion;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural Command tests for {@link GetAllContactosCommand}.
 *
 * <p>The Command owns its input contract:
 * <ul>
 *   <li>{@code actorUsuarioId} is mandatory (security scope) and is
 *       preserved exactly as a non-null {@link UUID}. A null actor
 *       fails the Command contract because identity is the entry
 *       point of the actor-scoped persistence query — it is never a
 *       filter.</li>
 *   <li>Optional text fields ({@code search}, {@code estadoRelacion},
 *       {@code comoNosConocio}) collapse to {@code null} when blank or
 *       whitespace-only.</li>
 *   <li>A present {@code estadoRelacion} must equal an
 *       {@link EstadoRelacion} {@code name()} exactly; any other value
 *       is rejected during Command construction.</li>
 *   <li>A present {@code maxResults} must be a positive integer.</li>
 * </ul>
 *
 * <p>The Service no longer performs any of this structural validation;
 * it only converts the already-validated String to a Domain enum and
 * the already-present UUID to {@code UsuarioId}.
 */
class GetAllContactosCommandTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");

    // ── Mandatory actor scope ──────────────────────────────────────

    @Test
    void rejectsNullActorUsuarioIdDuringConstruction() {
        InvalidGetAllContactosCommandException error = assertThrows(
                InvalidGetAllContactosCommandException.class,
                () -> new GetAllContactosCommand(
                        null, null, null, null, null, null, null));

        assertEquals(
                InvalidGetAllContactosCommandException.Reason.MISSING_ACTOR,
                error.getReason());
        assertNull(error.getRejectedValue(),
                "Null-actor rejection has no rejected value to surface");
        assertTrue(error.getMessage().contains("MISSING_ACTOR"),
                "Missing-actor message must name the reason");
        assertNull(error.getCause(),
                "Predicate-originated rejection carries no synthetic cause");
    }

    @Test
    void preservesPresentActorUsuarioIdVerbatim() {
        GetAllContactosCommand cmd = new GetAllContactosCommand(
                ACTOR, null, null, null, null, null, null);

        assertEquals(ACTOR, cmd.actorUsuarioId());
        assertNotNull(cmd.actorUsuarioId(),
                "actorUsuarioId is mandatory and must be preserved exactly as supplied");
    }

    // ── Optional normalization ─────────────────────────────────────

    @Test
    void normalizesNullSearchToAbsent() {
        GetAllContactosCommand cmd = new GetAllContactosCommand(
                ACTOR, null, null, null, null, null, null);

        assertNull(cmd.search());
    }

    @Test
    void normalizesBlankAndWhitespaceSearchToAbsent() {
        GetAllContactosCommand blank = new GetAllContactosCommand(
                ACTOR, "", null, null, null, null, null);
        GetAllContactosCommand whitespace = new GetAllContactosCommand(
                ACTOR, "   ", null, null, null, null, null);
        GetAllContactosCommand tabNewline = new GetAllContactosCommand(
                ACTOR, "\t\n", null, null, null, null, null);

        assertNull(blank.search());
        assertNull(whitespace.search());
        assertNull(tabNewline.search());
    }

    @Test
    void normalizesNullAndBlankComoNosConocioToAbsent() {
        GetAllContactosCommand blank = new GetAllContactosCommand(
                ACTOR, null, null, null, null, "", null);
        GetAllContactosCommand whitespace = new GetAllContactosCommand(
                ACTOR, null, null, null, null, "   ", null);

        assertNull(blank.comoNosConocio());
        assertNull(whitespace.comoNosConocio());
    }

    @Test
    void keepsTrimmedNonBlankOptionalTextUnchanged() {
        GetAllContactosCommand cmd = new GetAllContactosCommand(
                ACTOR, " Alice ", null, null, null, " LinkedIn ", null);

        assertEquals("Alice", cmd.search());
        assertEquals("LinkedIn", cmd.comoNosConocio());
    }

    // ── Exact EstadoRelacion.name() membership ─────────────────────

    @Test
    void acceptsEveryValidEstadoRelacionNameExactly() {
        for (EstadoRelacion value : EstadoRelacion.values()) {
            GetAllContactosCommand cmd = new GetAllContactosCommand(
                    ACTOR, null, value.name(), null, null, null, null);

            assertEquals(value.name(), cmd.estadoRelacion());
        }
    }

    @Test
    void acceptsNullEstadoRelacionAsAbsentFilter() {
        GetAllContactosCommand cmd = new GetAllContactosCommand(
                ACTOR, null, null, null, null, null, null);

        assertNull(cmd.estadoRelacion());
    }

    @Test
    void rejectsNonBlankEstadoRelacionNotEqualToAnyEnumName() {
        InvalidGetAllContactosCommandException error = assertThrows(
                InvalidGetAllContactosCommandException.class,
                () -> new GetAllContactosCommand(
                        ACTOR, null, "DESCONOCIDO", null, null, null, null));

        assertEquals(
                InvalidGetAllContactosCommandException.Reason.INVALID_ESTADO_RELACION,
                error.getReason());
        assertEquals("DESCONOCIDO", error.getRejectedValue());
        assertTrue(error.getMessage().contains("INVALID_ESTADO_RELACION"));
        assertTrue(error.getMessage().contains("DESCONOCIDO"));
        assertNull(error.getCause(),
                "Predicate-originated rejection carries no synthetic cause");
    }

    @Test
    void rejectsLowercaseEstadoRelacionAsCaseMismatchedMembership() {
        InvalidGetAllContactosCommandException error = assertThrows(
                InvalidGetAllContactosCommandException.class,
                () -> new GetAllContactosCommand(
                        ACTOR, null, "activo", null, null, null, null));

        assertEquals(
                InvalidGetAllContactosCommandException.Reason.INVALID_ESTADO_RELACION,
                error.getReason());
        assertEquals("activo", error.getRejectedValue());
    }

    // ── maxResults positivity ──────────────────────────────────────

    @Test
    void acceptsPositiveMaxResults() {
        GetAllContactosCommand cmd = new GetAllContactosCommand(
                ACTOR, null, null, null, null, null, 20);

        assertEquals(20, cmd.maxResults());
    }

    @Test
    void rejectsZeroMaxResultsDuringConstruction() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new GetAllContactosCommand(
                        ACTOR, null, null, null, null, null, 0));

        assertEquals("maxResults must be positive", error.getMessage());
    }

    @Test
    void rejectsNegativeMaxResultsDuringConstruction() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new GetAllContactosCommand(
                        ACTOR, null, null, null, null, null, -3));

        assertEquals("maxResults must be positive", error.getMessage());
    }

    // ── Diagnostics: framework-free Application exception ───────────

    @Test
    void commandRejectionIsAnIllegalArgumentExceptionForHttp400Handling() {
        InvalidGetAllContactosCommandException error = assertThrows(
                InvalidGetAllContactosCommandException.class,
                () -> new GetAllContactosCommand(
                        ACTOR, null, "DESCONOCIDO", null, null, null, null));

        assertSame(IllegalArgumentException.class, error.getClass().getSuperclass(),
                "Command-originated rejection must extend IllegalArgumentException "
                        + "so the existing GlobalExceptionHandler maps it to HTTP 400");
    }

    // ── UUID optionality preserved through normalization ───────────

    @Test
    void preservesPresentEmpresaIdAndResponsableId() {
        UUID empresaId = UUID.randomUUID();
        UUID responsableId = UUID.randomUUID();

        GetAllContactosCommand cmd = new GetAllContactosCommand(
                ACTOR, null, null, empresaId, responsableId, null, null);

        assertEquals(empresaId, cmd.empresaId());
        assertEquals(responsableId, cmd.responsableId());
    }
}
