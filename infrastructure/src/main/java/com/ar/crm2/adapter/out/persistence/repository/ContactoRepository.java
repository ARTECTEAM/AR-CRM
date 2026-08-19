package com.ar.crm2.adapter.out.persistence.repository;

import com.ar.crm2.adapter.out.persistence.entity.ContactoEntity;
import com.ar.crm2.model.enums.EstadoRelacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Contacto persistence.
 */
@Repository
public interface ContactoRepository extends JpaRepository<ContactoEntity, String> {

    /**
     * Checks whether any Tratos are associated with the given Contacto.
     * Uses JPQL over TratoEntity (not native SQL) per project conventions.
     */
    @Query("""
        SELECT COUNT(t) > 0
        FROM TratoEntity t
        WHERE t.contactoId = :contactoId
        """)
    boolean existsTratosByContactoId(@Param("contactoId") String contactoId);

    /**
     * One atomic actor-scoped, optionally filtered, deterministically
     * ordered search.
     *
     * <p>The {@code actor} predicate is the mandatory security scope:
     * every returned row must satisfy
     * {@code (creadoPor = actor OR responsableId = actor)}. The
     * remaining parameters are OPTIONAL filters and intersect the
     * scope (an optional {@code responsableId} narrows the visible
     * set but never replaces actor scope).
     *
     * <p>Order is fixed in JPQL ({@code creadoEn DESC, id ASC}) so the
     * deterministic order is enforced even when the caller passes an
     * unpaged {@link Pageable}. The {@link Pageable} only contributes
     * a database-level {@code LIMIT} when the caller needs a cap; the
     * caller's {@code Sort} must be {@code Sort.unsorted()} so it does
     * not duplicate the JPQL {@code ORDER BY}.
     */
    @Query("""
        SELECT c FROM ContactoEntity c
        WHERE (c.creadoPor = :actor OR c.responsableId = :actor)
          AND (:search IS NULL
               OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!'
               OR LOWER(c.correo) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!'
               OR LOWER(c.telefono) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!'
               OR LOWER(c.cargo) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!')
          AND (:estadoRelacion IS NULL OR c.estadoRelacion = :estadoRelacion)
          AND (:empresaId IS NULL OR c.empresaId = :empresaId)
          AND (:responsableId IS NULL OR c.responsableId = :responsableId)
          AND (:comoNosConocio IS NULL
               OR LOWER(TRIM(c.comoNosConocio)) = LOWER(TRIM(:comoNosConocio)))
        ORDER BY c.creadoEn DESC, c.id ASC
        """)
    List<ContactoEntity> searchScoped(
            @Param("actor") String actor,
            @Param("search") String search,
            @Param("estadoRelacion") EstadoRelacion estadoRelacion,
            @Param("empresaId") String empresaId,
            @Param("responsableId") String responsableId,
            @Param("comoNosConocio") String comoNosConocio,
            Pageable pageable
    );
}
