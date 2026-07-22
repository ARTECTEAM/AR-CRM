package com.ar.crm2.adapter.out.persistence.agent.tool;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AgentToolActionRepository extends JpaRepository<AgentToolActionEntity, String> {

    Optional<AgentToolActionEntity> findByOwnerIdAndId(String ownerId, String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select action from AgentToolActionEntity action "
            + "where action.ownerId = :ownerId and action.id = :id")
    Optional<AgentToolActionEntity> findByOwnerIdAndIdForUpdate(
            @Param("ownerId") String ownerId,
            @Param("id") String id);
}
