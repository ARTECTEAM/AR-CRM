package com.ar.crm2.adapter.out.persistence.repository;

import com.ar.crm2.adapter.out.persistence.entity.TableroEntity;
import com.ar.crm2.model.enums.TipoTablero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    /** Returns every board referencing a catalog column without loading lazy board collections. */
    @Query("""
        SELECT t.id
        FROM TableroEntity t
        JOIN t.columnasTablero ct
        WHERE ct.columnaId = :columnaId
        """)
    List<String> findBoardIdsByColumnasTableroColumnaId(@Param("columnaId") String columnaId);
}
