package com.ar.crm2.adapter.out.persistence.repository;

import com.ar.crm2.adapter.out.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for Usuario persistence.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, String> {

    @Query("SELECT COUNT(u) > 0 FROM UsuarioEntity u WHERE u.rolId = :rolId")
    boolean existsByRolId(@Param("rolId") String rolId);

    boolean existsByKeycloakId(String keycloakId);

    java.util.Optional<UsuarioEntity> findByKeycloakId(String keycloakId);

    java.util.Optional<UsuarioEntity> findByCorreo(String correo);

    java.util.List<UsuarioEntity> findByActivoTrue();

    long countByActivoTrueAndRolIdIn(java.util.Collection<String> roleIds);

    long countByActivoTrueAndRolId(String roleId);
}
