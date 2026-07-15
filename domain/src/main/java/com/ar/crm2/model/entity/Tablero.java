package com.ar.crm2.model.entity;

import com.ar.crm2.exception.ColumnaConFichasNoPuedeEliminarseException;
import com.ar.crm2.exception.ColumnaNoPerteneceAlTableroException;
import com.ar.crm2.exception.ColumnaYaExisteEnTableroException;
import com.ar.crm2.exception.InvariantViolationException;
import com.ar.crm2.exception.TipoTableroMismatchException;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.TableroId;
import com.ar.crm2.shared.DomainAssert;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import com.ar.crm2.model.enums.TipoTablero;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Domain entity for Tablero.
 *
 * <p>Identity: TableroId.
 * <p>Equality: by id only.
 * <p>Constructor is private; use static factory methods create() and reconstitute().
 *
 * <p>Columns are stored as {@link ColumnaTablero} contextual wrappers that pair
 * each {@link Columna} with its board-specific {@code limiteWip}.
 *
 * <h2>Responsibility split</h2>
 * <p>This aggregate owns the business rules for boards:
 * <ul>
 *   <li>The default board shape (which {@link ColumnaTablero} slots a board
 *       starts with) and the contextual WIP limits for each slot are owned
 *       here via {@link #requiredDefaultColumns(TipoTablero)}.</li>
 *   <li>{@link ColumnaTablero} owns the per-board WIP invariant
 *       ({@code limiteWip > 0}) and the total valor estimado semantics.</li>
 *   <li>{@link Columna} owns the catalog data and naming/duplicate rules.</li>
 * </ul>
 * <p>Authorization (who may create a board) is an application/security
 * concern and is NOT modeled here.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Tablero {

    @EqualsAndHashCode.Include
    private final TableroId id;

    private final String nombre;
    private final String descripcion;
    private final List<ColumnaTablero> columnasTablero;
    private final TipoTablero tipoTablero;
    private final LocalDateTime creadoEn;

    // ── Factory ──────────────────────────────────────────────────

    /**
     * Validates structural invariants for a column collection.
     * Ensures: list and elements not null, no duplicate ColumnaId values,
     * all columns match the given TipoTablero.
     *
     * <p>Note: Board-ownership check (columna.getTableroId() == this.id) is REMOVED
     * because Columna is now a catalog entity decoupled from board ownership.
     * Assignment is validated via tipoTablero match only.
     *
     * @param columnasTablero the column list to validate (must not be null, no null elements)
     * @param tipoTablero    the expected TipoTablero (used to validate each column's tipoTablero)
     * @throws InvariantViolationException if any structural invariant is violated (nulls, duplicates, ownership)
     * @throws TipoTableroMismatchException if any column has incompatible tipoTablero
     */
    private static void validarColumnasDelTablero(
        List<ColumnaTablero> columnasTablero,
        TipoTablero tipoTablero
    ) {
        if (columnasTablero == null) {
            throw new InvariantViolationException("La lista de columnas no puede ser null.");
        }
        for (int i = 0; i < columnasTablero.size(); i++) {
            if (columnasTablero.get(i) == null) {
                throw new InvariantViolationException("La columna en posición " + i + " no puede ser null.");
            }
        }

        Set<ColumnaId> seenIds = new HashSet<>();
        for (ColumnaTablero ct : columnasTablero) {
            ColumnaId id = ct.getColumnaId();
            if (seenIds.contains(id)) {
                throw new InvariantViolationException("La lista de columnas contiene IDs duplicados.");
            }
            seenIds.add(id);
        }

        // Board-ownership check removed: Columna is now a catalog, not board-owned.
        // Assignment validation is done via tipoTablero match in agregarColumnaTablero.

        for (ColumnaTablero ct : columnasTablero) {
            if (!ct.getTipoTablero().equals(tipoTablero)) {
                throw new TipoTableroMismatchException();
            }
        }
    }

    private static void validarDefaultColumnSpecs(
        List<ColumnaTablero> columnasTablero,
        TipoTablero tipoTablero
    ) {
        List<DefaultColumnSpec> requiredSpecs = requiredDefaultColumns(tipoTablero);
        if (columnasTablero.size() != requiredSpecs.size()) {
            throw new InvariantViolationException(
                "Un tablero debe crearse con exactamente " + requiredSpecs.size() + " columnas predeterminadas."
            );
        }
        for (int i = 0; i < requiredSpecs.size(); i++) {
            Integer actualWip = columnasTablero.get(i).getLimiteWip();
            int expectedWip = requiredSpecs.get(i).defaultLimiteWip();
            if (!Integer.valueOf(expectedWip).equals(actualWip)) {
                throw new InvariantViolationException(
                    "La columna predeterminada en posición " + i + " debe tener limiteWip " + expectedWip + "."
                );
            }
        }
    }

    /**
     * Creates a new Tablero.
     * Generates id and creadoEn internally.
     *
     * <p>Authorization: any authenticated user may create a Tablero. Creation
     * role semantics live in the application/security layer; the domain does
     * not require a {@code SuperUsuarioId} for creation. The application
     * service is responsible for resolving the actor from the authenticated
     * context and for assembling the default {@link ColumnaTablero} list
     * through the catalog ports.
     *
     * <p>Note: structural validation (no nulls, no duplicate IDs, tipoTablero
     * match) is delegated to the centralized helper.
     *
     * @param nombre       mandatory - board name
     * @param descripcion  mandatory - board description
     * @param tipoTablero  mandatory - type of board (TAREAS or TRATOS)
     * @param columnasTablero mandatory - the contextual column assignments
     *                                 for this board (catalog ColumnaId +
     *                                 WIP context). May be empty only if the
     *                                 caller explicitly accepts a board
     *                                 without contextual columns (the existing
     *                                 invariant still validates each entry).
     */
    public static Tablero create(
        String nombre,
        String descripcion,
        TipoTablero tipoTablero,
        List<ColumnaTablero> columnasTablero
    ) {
        DomainAssert.notNull(tipoTablero, "tipoTablero");
        validarColumnasDelTablero(columnasTablero, tipoTablero);
        validarDefaultColumnSpecs(columnasTablero, tipoTablero);
        DomainAssert.lengthBetween(nombre, "nombre", 1, 100);
        DomainAssert.notBlank(descripcion, "descripcion");

        return new Tablero(
            TableroId.create(),
            nombre.trim(),
            descripcion.trim(),
            List.copyOf(columnasTablero),
            tipoTablero,
            LocalDateTime.now()
        );
    }

    /**
     * Returns the list of default column specifications for the given board
     * type. The list is the canonical board shape for {@link TipoTablero#TAREAS}
     * and {@link TipoTablero#TRATOS} boards.
     *
     * <p>Each specification carries the catalog column name and the default
     * WIP limit that the board shape assigns. The application service
     * resolves each name to an existing PREDETERMINADA catalog column
     * (or creates one if missing) and pairs it with the WIP limit from
     * the spec to build a {@link ColumnaTablero} entry.
     *
     * <p>Default board shape:
     * <ul>
     *   <li>TAREAS: Pendiente (WIP 5), En Curso (WIP 3), Finalizada (WIP 5), Cancelada (WIP 5)</li>
     *   <li>TRATOS: Nuevo (WIP 10), Calificado (WIP 10), Propuesta (WIP 10), Negociación (WIP 10)</li>
     * </ul>
     *
     * <p>This is a domain behavior because the board shape (which columns a
     * board must have by default and their contextual WIP limits) is a
     * business rule, not a transport/orchestration concern.
     *
     * @param tipo the board type
     * @return immutable list of 4 default column specifications for the type
     * @throws com.ar.crm2.exception.InvariantViolationException if tipo is null
     */
    public static List<DefaultColumnSpec> requiredDefaultColumns(TipoTablero tipo) {
        DomainAssert.notNull(tipo, "tipo");
        return switch (tipo) {
            case TAREAS -> List.of(
                new DefaultColumnSpec("Pendiente", 5),
                new DefaultColumnSpec("En Curso", 3),
                new DefaultColumnSpec("Finalizada", 5),
                new DefaultColumnSpec("Cancelada", 5)
            );
            case TRATOS -> List.of(
                new DefaultColumnSpec("Nuevo", 10),
                new DefaultColumnSpec("Calificado", 10),
                new DefaultColumnSpec("Propuesta", 10),
                new DefaultColumnSpec("Negociación", 10)
            );
        };
    }

    /**
     * Default column specification owned by the {@link Tablero} aggregate.
     * Carries the catalog column name and the contextual WIP limit that the
     * board shape assigns to that column.
     *
     * <p>This is a value object, not a persisted entity.
     */
    public record DefaultColumnSpec(String name, int defaultLimiteWip) {
        public DefaultColumnSpec {
            DomainAssert.lengthBetween(name, "name", 1, 80);
            name = name.trim();
            if (defaultLimiteWip <= 0) {
                throw new InvariantViolationException("defaultLimiteWip debe ser mayor que cero.");
            }
        }
    }

    /**
     * Reconstitutes an existing Tablero from persistence.
     */
    public static Tablero reconstitute(
        TableroId id,
        String nombre,
        String descripcion,
        List<ColumnaTablero> columnasTablero,
        TipoTablero tipoTablero,
        LocalDateTime creadoEn
    ) {
        DomainAssert.notNull(id, "id");
        DomainAssert.lengthBetween(nombre, "nombre", 1, 100);
        DomainAssert.notBlank(descripcion, "descripcion");
        DomainAssert.notNull(columnasTablero, "columnasTablero");
        List<ColumnaTablero> resolvedColumnasTablero = List.copyOf(columnasTablero);
        DomainAssert.notNull(tipoTablero, "tipoTablero");
        DomainAssert.notNull(creadoEn, "creadoEn");

        validarColumnasDelTablero(resolvedColumnasTablero, tipoTablero);

        return new Tablero(
            id,
            nombre.trim(),
            descripcion.trim(),
            resolvedColumnasTablero,
            tipoTablero,
            creadoEn
        );
    }

    // ── Domain Behavior ────────────────────────────────────────────

    /**
     * Updates the editable fields (nombre, descripcion) of this Tablero and
     * returns a new immutable Tablero with the change applied.
     *
     * <p>All other fields (id, columnasTablero, tipoTablero, creadoEn) are
     * preserved exactly. Validation mirrors the field rules used in
     * {@link #create(String, String, TipoTablero, List)}: nombre 1-100 chars,
     * descripcion non-blank.
     *
     * <p>This is the domain edit behavior. The application service should
     * call this method instead of {@link #reconstitute(...)} to apply edits;
     * {@code reconstitute} is reserved for persistence hydration, not edit
     * orchestration.
     *
     * @param nuevoNombre      the new board name, 1-100 chars
     * @param nuevaDescripcion the new board description, non-blank
     * @return a new Tablero instance with the updated editable fields
     * @throws com.ar.crm2.exception.InvariantViolationException if the
     *         nombre/descripcion validation fails
     */
    public Tablero editarDatos(String nuevoNombre, String nuevaDescripcion) {
        DomainAssert.lengthBetween(nuevoNombre, "nombre", 1, 100);
        DomainAssert.notBlank(nuevaDescripcion, "descripcion");
        return new Tablero(
            this.id,
            nuevoNombre.trim(),
            nuevaDescripcion.trim(),
            this.columnasTablero,
            this.tipoTablero,
            this.creadoEn
        );
    }

    /**
     * Adds a column to this board.
     *
     * <p>Note: Board-ownership check is REMOVED. Columna is now a catalog entity
     * decoupled from board ownership. Assignment is validated via tipoTablero match only.
     *
     * Rules:
     * - columnaTablero is required.
     * - The column cannot already exist in this tablero (by ColumnaId identity).
     * - The column tipoTablero must match this board's tipoTablero.
     * - The column is appended at the end of the column list.
     *
     * @param columnaTablero the contextual column to add (required, contains columnaId + context)
     * @return a new Tablero with the column appended
     * @throws ColumnaYaExisteEnTableroException if column is already in this tablero
     * @throws TipoTableroMismatchException if column tipoTablero does not match this tablero
     */
    public Tablero agregarColumnaTablero(ColumnaTablero columnaTablero) {
        DomainAssert.notNull(columnaTablero, "columnaTablero");

        ColumnaId columnaId = columnaTablero.getColumnaId();
        boolean noDuplicada = columnasTablero.stream().noneMatch(ct -> ct.getColumnaId().equals(columnaId));
        if (!noDuplicada) {
            throw new ColumnaYaExisteEnTableroException();
        }

        // Validate tipoTablero match directly from ColumnaTablero (no longer from wrapped Columna)
        if (!columnaTablero.getTipoTablero().equals(this.tipoTablero)) {
            throw new TipoTableroMismatchException();
        }

        List<ColumnaTablero> nuevasColumnas = new ArrayList<>(columnasTablero);
        nuevasColumnas.add(columnaTablero);

        validarColumnasDelTablero(nuevasColumnas, this.tipoTablero);

        return new Tablero(
            this.id,
            this.nombre,
            this.descripcion,
            List.copyOf(nuevasColumnas),
            this.tipoTablero,
            this.creadoEn
        );
    }

    /**
     * Removes a column from this board.
     *
     * Rules:
     * - columnaId is required.
     * - The column must belong to this tablero (by ColumnaId identity).
     * - The column must not contain fichas (tieneFichas must be false).
     * - The relative order of remaining columns is preserved.
     *
     * @param columnaId  the id of the column to remove (required)
     * @param tieneFichas true if the column contains fichas, false otherwise
     * @return a new Tablero with the column removed
     * @throws InvariantViolationException if columnaId is null
     * @throws ColumnaNoPerteneceAlTableroException if columnaId does not belong to this tablero
     * @throws ColumnaConFichasNoPuedeEliminarseException if tieneFichas is true
     */
    public Tablero eliminarColumnaDelTablero(ColumnaId columnaId, boolean tieneFichas) {
        DomainAssert.notNull(columnaId, "columnaId");

        boolean columnaExiste = columnasTablero.stream().anyMatch(ct -> ct.getColumnaId().equals(columnaId));
        if (!columnaExiste) {
            throw new ColumnaNoPerteneceAlTableroException();
        }

        if (tieneFichas) {
            throw new ColumnaConFichasNoPuedeEliminarseException();
        }

        List<ColumnaTablero> nuevasColumnas = new ArrayList<>(columnasTablero);
        nuevasColumnas.removeIf(ct -> ct.getColumnaId().equals(columnaId));

        return new Tablero(
            this.id,
            this.nombre,
            this.descripcion,
            List.copyOf(nuevasColumnas),
            this.tipoTablero,
            this.creadoEn
        );
    }

    /**
     * Reorders the columns of this board to match the given new order.
     *
     * Rules:
     * - nuevoOrden is required and must not be empty.
     * - nuevoOrden must contain exactly the same ColumnaId values currently present in this Tablero.
     * - nuevoOrden must not contain duplicate ColumnaId values.
     * - All IDs in nuevoOrden must belong to this Tablero.
     * - TipoColumna is NOT considered during validation or reordering.
     *
     * @param nuevoOrden the desired column order (required, must contain all current column IDs exactly once)
     * @return a new Tablero with columns in the requested order
     * @throws InvariantViolationException if nuevoOrden is null, empty, or contains wrong number of IDs
     * @throws InvariantViolationException if nuevoOrden contains duplicate IDs
     * @throws ColumnaNoPerteneceAlTableroException if nuevoOrden contains an ID that does not belong to this Tablero
     */
    public Tablero reordenarColumnas(List<ColumnaId> nuevoOrden) {
        DomainAssert.notNull(nuevoOrden, "nuevoOrden");

        if (nuevoOrden.isEmpty()) {
            throw new InvariantViolationException("El campo nuevoOrden no puede estar vacío.");
        }

        if (nuevoOrden.size() != columnasTablero.size()) {
            throw new InvariantViolationException(
                "El nuevo orden debe contener exactamente " + columnasTablero.size() + " columnas, pero contiene " + nuevoOrden.size() + "."
            );
        }

        Set<ColumnaId> uniqueIds = new HashSet<>(nuevoOrden);
        if (uniqueIds.size() != nuevoOrden.size()) {
            throw new InvariantViolationException("El nuevo orden no puede contener columnas duplicadas.");
        }

        Map<ColumnaId, ColumnaTablero> columnasMap = new LinkedHashMap<>();
        for (ColumnaTablero ct : columnasTablero) {
            columnasMap.put(ct.getColumnaId(), ct);
        }

        for (ColumnaId id : nuevoOrden) {
            if (!columnasMap.containsKey(id)) {
                throw new ColumnaNoPerteneceAlTableroException();
            }
        }

        List<ColumnaTablero> columnasReordenadas = new ArrayList<>(nuevoOrden.size());
        for (ColumnaId id : nuevoOrden) {
            columnasReordenadas.add(columnasMap.get(id));
        }

        return new Tablero(
            this.id,
            this.nombre,
            this.descripcion,
            List.copyOf(columnasReordenadas),
            this.tipoTablero,
            this.creadoEn
        );
    }
}
