package com.ar.crm2.model.entity;

import com.ar.crm2.exception.ColumnaConFichasNoPuedeEliminarseException;
import com.ar.crm2.exception.ColumnaNoPerteneceAlTableroException;
import com.ar.crm2.exception.ColumnaYaExisteEnTableroException;
import com.ar.crm2.exception.InvariantViolationException;
import com.ar.crm2.model.enums.TipoColumna;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.SuperUsuarioId;
import com.ar.crm2.model.vo.TableroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Tablero} column removal.
 * Spring-free — pure domain unit tests.
 */
class TableroTest {

    private static final TableroId TABLERO_ID = TableroId.from(UUID.randomUUID());
    private static final SuperUsuarioId SUPER_USUARIO_ID = SuperUsuarioId.create();
    private static final TipoTablero TIPO = TipoTablero.TAREAS;

    private Tablero buildTableroConColumnasTablero(List<ColumnaTablero> columnasTablero) {
        return Tablero.reconstitute(
            TABLERO_ID,
            "Tablero Test",
            "Descripción test",
            columnasTablero,
            TIPO,
            LocalDateTime.now()
        );
    }

    private ColumnaTablero columnaTableroPredeterminada(String nombre, ColumnaId id, int limiteWip) {
        return ColumnaTablero.create(
            id,
            TIPO,
            limiteWip,
            null,
            BigDecimal.ZERO
        );
    }

    private ColumnaTablero columnaTableroPersonalizada(String nombre, ColumnaId id, int limiteWip) {
        return ColumnaTablero.create(
            id,
            TIPO,
            limiteWip,
            null,
            BigDecimal.ZERO
        );
    }

    @Nested
    @DisplayName("requiredDefaultColumns")
    class RequiredDefaultColumns {

        @Test
        @DisplayName("TRATOS defaults are pipeline stages, not commercial outcomes")
        void tratosDefaults_arePipelineStages() {
            List<Tablero.DefaultColumnSpec> specs = Tablero.requiredDefaultColumns(TipoTablero.TRATOS);

            assertThat(specs)
                .extracting(Tablero.DefaultColumnSpec::name)
                .containsExactly("Nuevo", "Calificado", "Propuesta", "Negociación");
        }

        @Test
        @DisplayName("TAREAS defaults remain operational workflow stages")
        void tareasDefaults_areOperationalWorkflowStages() {
            List<Tablero.DefaultColumnSpec> specs = Tablero.requiredDefaultColumns(TipoTablero.TAREAS);

            assertThat(specs)
                .extracting(Tablero.DefaultColumnSpec::name)
                .containsExactly("Pendiente", "En Curso", "Finalizada", "Cancelada");
        }
    }

    @Nested
    @DisplayName("eliminarColumnaDelTablero")
    class EliminarColumnaDelTablero {

        @Test
        @DisplayName("successfully remove an empty column and preserve order of remaining columns")
        void removeEmptyColumn_success() {
            ColumnaId id1 = ColumnaId.create();
            ColumnaId id2 = ColumnaId.create();
            ColumnaId id3 = ColumnaId.create();

            List<ColumnaTablero> columnasTablero = List.of(
                columnaTableroPredeterminada("Por Hacer", id1, 5),
                columnaTableroPersonalizada("En Progreso", id2, 3),
                columnaTableroPredeterminada("Hecho", id3, 5)
            );

            Tablero tablero = buildTableroConColumnasTablero(columnasTablero);

            Tablero result = tablero.eliminarColumnaDelTablero(id2, false);

            assertThat(result.getColumnasTablero()).hasSize(2);
            assertThat(result.getColumnasTablero().get(0).getColumnaId()).isEqualTo(id1);
            assertThat(result.getColumnasTablero().get(1).getColumnaId()).isEqualTo(id3);
            assertThat(result.getId()).isEqualTo(TABLERO_ID);
            assertThat(result.getNombre()).isEqualTo("Tablero Test");
            assertThat(result.getTipoTablero()).isEqualTo(TIPO);
        }

        @Test
        @DisplayName("throw ColumnaNoPerteneceAlTableroException when columnaId not in tablero")
        void removeColumnaNotBelongs_throws() {
            ColumnaId existingId = ColumnaId.create();
            ColumnaId absentId = ColumnaId.create();

            List<ColumnaTablero> columnasTablero = List.of(
                columnaTableroPredeterminada("Por Hacer", existingId, 5)
            );

            Tablero tablero = buildTableroConColumnasTablero(columnasTablero);

            assertThatThrownBy(() -> tablero.eliminarColumnaDelTablero(absentId, false))
                .isInstanceOf(ColumnaNoPerteneceAlTableroException.class);
        }

        @Test
        @DisplayName("throw ColumnaConFichasNoPodeEliminarseException when tieneFichas is true")
        void removeColumnaConFichas_throws() {
            ColumnaId id = ColumnaId.create();

            List<ColumnaTablero> columnasTablero = List.of(
                columnaTableroPredeterminada("Por Hacer", id, 5)
            );

            Tablero tablero = buildTableroConColumnasTablero(columnasTablero);

            assertThatThrownBy(() -> tablero.eliminarColumnaDelTablero(id, true))
                .isInstanceOf(ColumnaConFichasNoPuedeEliminarseException.class);
        }

        @Test
        @DisplayName("throw InvariantViolationException when columnaId is null")
        void removeColumnaNull_throws() {
            List<ColumnaTablero> columnasTablero = List.of(
                columnaTableroPredeterminada("Por Hacer", ColumnaId.create(), 5)
            );

            Tablero tablero = buildTableroConColumnasTablero(columnasTablero);

            assertThatThrownBy(() -> tablero.eliminarColumnaDelTablero(null, false))
                .isInstanceOf(InvariantViolationException.class);
        }
    }

    @Nested
    @DisplayName("reordenarColumnas")
    class ReordenarColumnas {

        @Test
        @DisplayName("valid reorder returns new Tablero with exact requested order")
        void validReorder_exactOrder() {
            ColumnaId id1 = ColumnaId.create();
            ColumnaId id2 = ColumnaId.create();
            ColumnaId id3 = ColumnaId.create();

            List<ColumnaTablero> columnasTablero = List.of(
                columnaTableroPredeterminada("Por Hacer", id1, 5),
                columnaTableroPredeterminada("En Progreso", id2, 3),
                columnaTableroPredeterminada("Hecho", id3, 5)
            );

            Tablero tablero = buildTableroConColumnasTablero(columnasTablero);

            // Request order: id3, id1, id2
            List<ColumnaId> nuevoOrden = List.of(id3, id1, id2);

            Tablero result = tablero.reordenarColumnas(nuevoOrden);

            assertThat(result.getColumnasTablero()).hasSize(3);
            assertThat(result.getColumnasTablero().get(0).getColumnaId()).isEqualTo(id3);
            assertThat(result.getColumnasTablero().get(1).getColumnaId()).isEqualTo(id1);
            assertThat(result.getColumnasTablero().get(2).getColumnaId()).isEqualTo(id2);
        }

        @Test
        @DisplayName("null nuevoOrden throws InvariantViolationException")
        void nullOrden_throws() {
            List<ColumnaTablero> columnasTablero = List.of(
                columnaTableroPredeterminada("Por Hacer", ColumnaId.create(), 5)
            );

            Tablero tablero = buildTableroConColumnasTablero(columnasTablero);

            assertThatThrownBy(() -> tablero.reordenarColumnas(null))
                .isInstanceOf(InvariantViolationException.class);
        }

        @Test
        @DisplayName("empty list throws InvariantViolationException")
        void emptyList_throws() {
            List<ColumnaTablero> columnasTablero = List.of(
                columnaTableroPredeterminada("Por Hacer", ColumnaId.create(), 5)
            );

            Tablero tablero = buildTableroConColumnasTablero(columnasTablero);

            assertThatThrownBy(() -> tablero.reordenarColumnas(List.of()))
                .isInstanceOf(InvariantViolationException.class);
        }

        @Test
        @DisplayName("duplicate IDs throw InvariantViolationException")
        void duplicateIds_throws() {
            ColumnaId id1 = ColumnaId.create();
            ColumnaId id2 = ColumnaId.create();

            List<ColumnaTablero> columnasTablero = List.of(
                columnaTableroPredeterminada("Por Hacer", id1, 5),
                columnaTableroPredeterminada("Hecho", id2, 5)
            );

            Tablero tablero = buildTableroConColumnasTablero(columnasTablero);

            // id1 appears twice
            List<ColumnaId> nuevoOrden = List.of(id1, id1);

            assertThatThrownBy(() -> tablero.reordenarColumnas(nuevoOrden))
                .isInstanceOf(InvariantViolationException.class);
        }

        @Test
        @DisplayName("size mismatch throws InvariantViolationException")
        void sizeMismatch_throws() {
            ColumnaId id1 = ColumnaId.create();
            ColumnaId id2 = ColumnaId.create();
            ColumnaId id3 = ColumnaId.create();

            List<ColumnaTablero> columnasTablero = List.of(
                columnaTableroPredeterminada("Por Hacer", id1, 5),
                columnaTableroPredeterminada("En Progreso", id2, 3)
            );

            Tablero tablero = buildTableroConColumnasTablero(columnasTablero);

            // 3 IDs but tablero only has 2 columns
            List<ColumnaId> nuevoOrden = List.of(id1, id2, id3);

            assertThatThrownBy(() -> tablero.reordenarColumnas(nuevoOrden))
                .isInstanceOf(InvariantViolationException.class);
        }

        @Test
        @DisplayName("foreign/nonexistent ID throws ColumnaNoPerteneceAlTableroException")
        void foreignId_throws() {
            ColumnaId id1 = ColumnaId.create();
            ColumnaId id2 = ColumnaId.create();
            ColumnaId foreignId = ColumnaId.create();

            List<ColumnaTablero> columnasTablero = List.of(
                columnaTableroPredeterminada("Por Hacer", id1, 5),
                columnaTableroPredeterminada("En Progreso", id2, 3)
            );

            Tablero tablero = buildTableroConColumnasTablero(columnasTablero);

            // Same cardinality as existing columns, but id2 replaced by foreign ID
            List<ColumnaId> nuevoOrden = List.of(id1, foreignId);

            assertThatThrownBy(() -> tablero.reordenarColumnas(nuevoOrden))
                .isInstanceOf(ColumnaNoPerteneceAlTableroException.class);
        }

        @Test
        @DisplayName("reorder works with mixed PREDETERMINADA/PERSONALIZADA columns — TipoColumna is ignored during reorder")
        void validReorder_mixedTipoColumna() {
            ColumnaId id1 = ColumnaId.create();
            ColumnaId id2 = ColumnaId.create();
            ColumnaId id3 = ColumnaId.create();

            // Mix PREDETERMINADA and PERSONALIZADA columns in the same Tablero.
            // Tablero.create() would reject this mix, but reconstitute() (used by buildTableroConColumnasTablero)
            // allows any column types — which is the path used by persistence reconstitution.
            List<ColumnaTablero> columnasTablero = List.of(
                columnaTableroPredeterminada("Por Hacer", id1, 5),
                columnaTableroPersonalizada("En Progreso", id2, 3),
                columnaTableroPredeterminada("Hecho", id3, 5)
            );

            Tablero tablero = buildTableroConColumnasTablero(columnasTablero);

            // Request reverse order: id3, id2, id1
            List<ColumnaId> nuevoOrden = List.of(id3, id2, id1);

            Tablero result = tablero.reordenarColumnas(nuevoOrden);

            // Verify exact order was applied
            assertThat(result.getColumnasTablero()).hasSize(3);
            assertThat(result.getColumnasTablero().get(0).getColumnaId()).isEqualTo(id3);
            assertThat(result.getColumnasTablero().get(1).getColumnaId()).isEqualTo(id2);
            assertThat(result.getColumnasTablero().get(2).getColumnaId()).isEqualTo(id1);
        }

        @Test
        @DisplayName("original Tablero remains unchanged — immutable behavior")
        void originalRemainsUnchanged() {
            ColumnaId id1 = ColumnaId.create();
            ColumnaId id2 = ColumnaId.create();

            List<ColumnaTablero> columnasTablero = List.of(
                columnaTableroPredeterminada("Por Hacer", id1, 5),
                columnaTableroPredeterminada("Hecho", id2, 5)
            );

            Tablero tablero = buildTableroConColumnasTablero(columnasTablero);

            List<ColumnaId> originalOrder = List.of(id1, id2);
            Tablero result = tablero.reordenarColumnas(originalOrder);

            // Original unchanged
            assertThat(tablero.getColumnasTablero()).hasSize(2);
            assertThat(tablero.getColumnasTablero().get(0).getColumnaId()).isEqualTo(id1);
            assertThat(tablero.getColumnasTablero().get(1).getColumnaId()).isEqualTo(id2);

            // Result is a different instance
            assertThat(result).isNotSameAs(tablero);

            // Result has reordered columns
            assertThat(result.getColumnasTablero().get(0).getColumnaId()).isEqualTo(id1);
            assertThat(result.getColumnasTablero().get(1).getColumnaId()).isEqualTo(id2);
        }
    }

    @Nested
    @DisplayName("agregarColumnaTablero")
    class AgregarColumnaTablero {

        @Test
        @DisplayName("successfully add a new column to the board")
        void addColumn_success() {
            ColumnaId id1 = ColumnaId.create();
            ColumnaId id2 = ColumnaId.create();

            List<ColumnaTablero> columnasTablero = List.of(
                columnaTableroPredeterminada("Por Hacer", id1, 5)
            );

            Tablero tablero = buildTableroConColumnasTablero(columnasTablero);

            Columna nuevaColumna = Columna.reconstitute(
                id2,
                "En Progreso",
                "#00FF00",
                TIPO,
                TipoColumna.PERSONALIZADA,
                false
            );
            ColumnaTablero nuevo = ColumnaTablero.create(
                id2,
                TIPO,
                3,
                null,
                BigDecimal.ZERO
            );

            Tablero result = tablero.agregarColumnaTablero(nuevo);

            assertThat(result.getColumnasTablero()).hasSize(2);
            assertThat(result.getColumnasTablero().get(0).getColumnaId()).isEqualTo(id1);
            assertThat(result.getColumnasTablero().get(1).getColumnaId()).isEqualTo(id2);
            // The bare Columna no longer carries limiteWip — it lives in ColumnaTablero
            assertThat(result.getColumnasTablero().get(1).getLimiteWip()).isEqualTo(3);
        }

        @Test
        @DisplayName("throw ColumnaYaExisteEnTableroException when adding duplicate column")
        void addDuplicate_throws() {
            ColumnaId id1 = ColumnaId.create();

            List<ColumnaTablero> columnasTablero = List.of(
                columnaTableroPredeterminada("Por Hacer", id1, 5)
            );

            Tablero tablero = buildTableroConColumnasTablero(columnasTablero);

            Columna mismaColumna = Columna.reconstitute(
                id1,
                "Por Hacer",
                "#FFFFFF",
                TIPO,
                TipoColumna.PREDETERMINADA,
                false
            );
            ColumnaTablero duplicada = ColumnaTablero.create(
                id1,
                TIPO,
                5,
                null,
                BigDecimal.ZERO
            );

            assertThatThrownBy(() -> tablero.agregarColumnaTablero(duplicada))
                .isInstanceOf(ColumnaYaExisteEnTableroException.class);
        }

        @Test
        @DisplayName("succeeds when adding a column with a different ColumnaId (no ownership check in current model)")
        void addForeign_succeeds() {
            ColumnaId id1 = ColumnaId.create();
            ColumnaId id2 = ColumnaId.create();

            List<ColumnaTablero> columnasTablero = List.of(
                columnaTableroPredeterminada("Por Hacer", id1, 5)
            );

            Tablero tablero = buildTableroConColumnasTablero(columnasTablero);

            // ColumnaTablero with a different ColumnaId (id2) — no ownership check in current model
            ColumnaTablero ajena = ColumnaTablero.create(
                id2,
                TIPO,
                3,
                null,
                BigDecimal.ZERO
            );

            Tablero result = tablero.agregarColumnaTablero(ajena);

            assertThat(result.getColumnasTablero()).hasSize(2);
            assertThat(result.getColumnasTablero().get(0).getColumnaId()).isEqualTo(id1);
            assertThat(result.getColumnasTablero().get(1).getColumnaId()).isEqualTo(id2);
        }
    }
}
