package com.ar.crm2.adapter.out.persistence.repository;

import com.ar.crm2.adapter.out.persistence.entity.TratoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * JPA repository for TratoEntity persistence operations.
 * Uses String as the id type to match the String ID boundary convention.
 */
public interface TratoRepository extends JpaRepository<TratoEntity, String>, JpaSpecificationExecutor<TratoEntity> {

    Optional<TratoEntity> findFirstByContactoIdOrderByCreadoEnDesc(String contactoId);
}
