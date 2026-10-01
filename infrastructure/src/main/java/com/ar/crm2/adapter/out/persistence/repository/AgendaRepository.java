package com.ar.crm2.adapter.out.persistence.repository;

import com.ar.crm2.adapter.out.persistence.entity.AgendaEntity;
import com.ar.crm2.model.enums.RecordatorioEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AgendaRepository extends JpaRepository<AgendaEntity, String> {
    List<AgendaEntity> findByCreadoPor(String creadoPor);

    boolean existsByIdAndCreadoPor(String id, String creadoPor);

    @Query("SELECT a FROM AgendaEntity a WHERE a.recordatorioHabilitado = true " +
           "AND a.recordatorioEstado IN :estados")
    List<AgendaEntity> findRemindersWithStatus(@Param("estados") List<RecordatorioEstado> estados);
}
