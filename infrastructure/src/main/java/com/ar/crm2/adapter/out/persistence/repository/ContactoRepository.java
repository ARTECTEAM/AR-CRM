package com.ar.crm2.adapter.out.persistence.repository;

import com.ar.crm2.adapter.out.persistence.entity.ContactoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
