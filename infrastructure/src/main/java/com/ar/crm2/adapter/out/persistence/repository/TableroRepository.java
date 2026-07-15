package com.ar.crm2.adapter.out.persistence.repository;

import com.ar.crm2.adapter.out.persistence.entity.TableroEntity;
import com.ar.crm2.model.enums.TipoTablero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for Tablero aggregate root persistence.
 * Uses String id matching the project convention (UUID stored as VARCHAR).
 */
@Repository
public interface TableroRepository extends JpaRepository<TableroEntity, String> {

    /**
     * Finds the first Tablero by tipoTablero.
     *
     * @param tipoTablero the type of board (TAREAS or TRATOS)
     * @return optional containing the first board of the given type, if any
     */
    java.util.Optional<TableroEntity> findFirstByTipoTablero(TipoTablero tipoTablero);

    /**
     * Checks whether a Columna is already assigned to the given Tablero.
     *
     * @param tableroId the string form of the TableroId
     * @param columnaId the string form of the ColumnaId
     * @return true if the column is already assigned to the board
     */
    boolean existsByIdAndColumnasTableroColumnaId(String tableroId, String columnaId);

    /**
     * Checks whether a Columna is assigned to any Tablero.
     *
     * @param columnaId the string form of the ColumnaId
     * @return true if the column is assigned to at least one board
     */
    boolean existsByColumnasTableroColumnaId(String columnaId);

    /**
     * Checks whether a Columna is assigned to at least one tablero of the
     * expected workflow type.
     *
     * @param columnaId the string form of the ColumnaId
     * @param tipoTablero the target board workflow type
     * @return true if the column is assigned to a board of that type
     */
    boolean existsByColumnasTableroColumnaIdAndColumnasTableroTipoTablero(
        String columnaId,
        TipoTablero tipoTablero
    );
}
