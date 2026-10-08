package com.ar.crm2.adapter.out.persistence.agent.repository;

import com.ar.crm2.adapter.out.persistence.agent.entity.AgentTurnRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentTurnRequestRepository extends JpaRepository<AgentTurnRequestEntity, String> {

    Optional<AgentTurnRequestEntity> findByOwnerIdAndIdempotencyKey(String ownerId, String idempotencyKey);

    Optional<AgentTurnRequestEntity> findByOwnerIdAndTurnIdAndOpaqueHandle(
        String ownerId,
        String turnId,
        String opaqueHandle
    );
}
