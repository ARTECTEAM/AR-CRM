package com.ar.crm2.adapter.out.persistence.repository;

import com.ar.crm2.adapter.out.persistence.entity.ContactoEntity;
import com.ar.crm2.adapter.out.persistence.entity.EmpresaEntity;
import com.ar.crm2.adapter.out.persistence.entity.TratoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for Empresa persistence.
 */
@Repository
public interface EmpresaRepository extends JpaRepository<EmpresaEntity, String>, JpaSpecificationExecutor<EmpresaEntity> {

    @Query("""
        SELECT COUNT(t) > 0
        FROM TratoEntity t
        JOIN ContactoEntity c ON t.contactoId = c.id
        WHERE c.empresaId = :empresaId
        """)
    boolean existsTratosByEmpresaId(@Param("empresaId") String empresaId);
}
