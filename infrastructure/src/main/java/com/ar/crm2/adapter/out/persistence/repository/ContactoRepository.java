package com.ar.crm2.adapter.out.persistence.repository;

import com.ar.crm2.adapter.out.persistence.entity.ContactoEntity;
import com.ar.crm2.model.enums.EstadoRelacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Contacto persistence.
 */
@Repository
public interface ContactoRepository extends JpaRepository<ContactoEntity, String>, JpaSpecificationExecutor<ContactoEntity> {

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
     * Usado al sincronizar el directorio de WhatsApp: evita crear contactos
     * duplicados cuando el mismo teléfono ya existe para la empresa.
     */
    Optional<ContactoEntity> findByEmpresaIdAndTelefono(String empresaId, String telefono);

    /**
     * Atomic actor-scoped contact query. Actor visibility is mandatory;
     * optional filters only narrow that set. Results are deterministic,
     * and Pageable contributes a database-level cap when requested.
     */
    @Query("""
        SELECT c FROM ContactoEntity c
        WHERE (:actor IS NULL OR c.creadoPor = :actor OR c.responsableId = :actor)
          AND (:empresaActor IS NULL OR EXISTS (
              SELECT e.id FROM EmpresaEntity e
              WHERE e.id = c.empresaId
                AND (e.creadoPor = :empresaActor OR e.responsableId = :empresaActor)
          ))
          AND (:search IS NULL
               OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!'
               OR (:includePrivateFields = true AND LOWER(c.correo) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!')
               OR (:includePrivateFields = true AND LOWER(c.telefono) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!')
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
            @Param("empresaActor") String empresaActor,
            @Param("search") String search,
            @Param("estadoRelacion") EstadoRelacion estadoRelacion,
            @Param("empresaId") String empresaId,
            @Param("responsableId") String responsableId,
            @Param("comoNosConocio") String comoNosConocio,
            @Param("includePrivateFields") boolean includePrivateFields,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(c) > 0
        FROM ContactoEntity c
        WHERE c.id = :contactoId
          AND (c.creadoPor = :actor OR c.responsableId = :actor)
        """)
    boolean isVisibleToActor(
            @Param("contactoId") String contactoId,
            @Param("actor") String actor
    );}
