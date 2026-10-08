package com.ar.crm2.adapter.out.persistence.repository;

import com.ar.crm2.adapter.out.persistence.entity.FichaEtiquetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the FichaEtiqueta owned relation.
 *
 * <p>Backs the two port operations that touch the join table directly
 * (independent of the Ficha aggregate boundary):
 * <ul>
 *   <li>{@link #countByEtiquetaId(String)} → {@code CountFichaEtiquetasByEtiquetaIdPort}</li>
 *   <li>{@link #deleteByEtiquetaId(String)} → {@code DeleteFichaEtiquetasByEtiquetaIdPort}</li>
 * </ul>
 *
 * <p>Read and write within the {@link com.ar.crm2.adapter.out.persistence.entity.FichaEntity}
 * aggregate boundary (i.e. save/load a Ficha together with its tags) is
 * handled by the standard {@code JpaRepository} save/findById methods on
 * {@code FichaEntity}'s one-to-many cascade.
 */
@Repository
public interface FichaEtiquetaRepository extends JpaRepository<FichaEtiquetaEntity, String> {

    /**
     * Counts all FichaEtiqueta rows that reference the given Etiqueta id.
     * Used by the delete flow to decide whether the catalog row is in use.
     */
    long countByEtiquetaId(String etiquetaId);

    /** Returns affected card IDs so authorization can be checked before a cross-board cascade. */
    @Query("SELECT DISTINCT fe.ficha.id FROM FichaEtiquetaEntity fe WHERE fe.etiquetaId = :etiquetaId")
    List<String> findFichaIdsByEtiquetaId(@Param("etiquetaId") String etiquetaId);

    /**
     * Bulk-deletes all FichaEtiqueta rows that reference the given Etiqueta id.
     * Used by the confirmed-delete flow to cascade the relation rows before
     * removing the catalog row. Both deletes MUST be wrapped in a single
     * transaction at the infrastructure boundary so the cascade is atomic.
     */
    @Modifying
    @Query("DELETE FROM FichaEtiquetaEntity fe WHERE fe.etiquetaId = :etiquetaId")
    int deleteByEtiquetaId(@Param("etiquetaId") String etiquetaId);
}
