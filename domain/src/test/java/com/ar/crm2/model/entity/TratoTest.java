package com.ar.crm2.model.entity;

import com.ar.crm2.exception.InvariantViolationException;
import com.ar.crm2.model.enums.TipoContrato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Trato} domain behavior.
 * Spring-free — pure domain unit tests.
 */
class TratoTest {

    private static final ContactoId CONTACTO_ID = ContactoId.create();
    private static final UsuarioId RESPONSABLE_ID = UsuarioId.create();
    private static final LocalDateTime AHORA = LocalDateTime.now();

    // ── create() ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Crear {

        @Test
        @DisplayName("succeeds when contactoId is provided")
        void crear_conContactoId_exitoso() {
            assertThatCode(() -> Trato.create(
                    CONTACTO_ID,
                    RESPONSABLE_ID,
                    "Venta de software",
                    new BigDecimal("50000.00"),
                    75,
                    LocalDate.now().plusDays(30),
                    TipoContrato.LICENCIA
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws when contactoId is null")
        void crear_conContactoIdNulo_lanzaExcepcion() {
            assertThatThrownBy(() -> Trato.create(
                    null,
                    RESPONSABLE_ID,
                    "Venta de software",
                    new BigDecimal("50000.00"),
                    75,
                    LocalDate.now().plusDays(30),
                    TipoContrato.LICENCIA
            ))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("contactoId");
        }

        @Test
        @DisplayName("throws when nombre is blank")
        void crear_conNombreBlanco_lanzaExcepcion() {
            assertThatThrownBy(() -> Trato.create(
                    CONTACTO_ID,
                    RESPONSABLE_ID,
                    "",
                    new BigDecimal("50000.00"),
                    75,
                    LocalDate.now().plusDays(30),
                    TipoContrato.LICENCIA
            ))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("nombre");
        }
    }

    // ── reconstitute() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reconstitute()")
    class Reconstituir {

        @Test
        @DisplayName("succeeds when contactoId is provided")
        void reconstituir_conContactoId_exitoso() {
            TratoId id = TratoId.create();
            LocalDateTime haceUnaSemana = AHORA.minusDays(7);

            assertThatCode(() -> Trato.reconstitute(
                    id,
                    CONTACTO_ID,
                    RESPONSABLE_ID,
                    "Venta de software",
                    new BigDecimal("50000.00"),
                    75,
                    LocalDate.now().plusDays(30),
                     TipoContrato.LICENCIA,
                     com.ar.crm2.model.enums.EstadoTrato.ABIERTO,
                     haceUnaSemana,
                    haceUnaSemana
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws when contactoId is null")
        void reconstituir_conContactoIdNulo_lanzaExcepcion() {
            TratoId id = TratoId.create();
            LocalDateTime haceUnaSemana = AHORA.minusDays(7);

            assertThatThrownBy(() -> Trato.reconstitute(
                    id,
                    null,
                    RESPONSABLE_ID,
                    "Venta de software",
                    new BigDecimal("50000.00"),
                    75,
                    LocalDate.now().plusDays(30),
                     TipoContrato.LICENCIA,
                     com.ar.crm2.model.enums.EstadoTrato.ABIERTO,
                     haceUnaSemana,
                    haceUnaSemana
            ))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("contactoId");
        }

        @Test
        @DisplayName("preserves contactoId when reconstituting")
        void reconstituir_preservaContactoId() {
            TratoId id = TratoId.create();
            LocalDateTime haceUnaSemana = AHORA.minusDays(7);

            Trato trato = Trato.reconstitute(
                    id,
                    CONTACTO_ID,
                    RESPONSABLE_ID,
                    "Venta de software",
                    new BigDecimal("50000.00"),
                    75,
                    LocalDate.now().plusDays(30),
                     TipoContrato.LICENCIA,
                     com.ar.crm2.model.enums.EstadoTrato.ABIERTO,
                     haceUnaSemana,
                    haceUnaSemana
            );

            assertThat(trato.getContactoId()).isEqualTo(CONTACTO_ID);
        }
    }
}
