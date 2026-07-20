package com.ar.crm2.adapter.out.persistence.agent.repository;

import com.ar.crm2.adapter.out.persistence.agent.entity.AgentTurnEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentTurnRepository extends JpaRepository<AgentTurnEntity, String> {

    Optional<AgentTurnEntity> findByIdAndConversationOwnerId(String id, String ownerId);
}
