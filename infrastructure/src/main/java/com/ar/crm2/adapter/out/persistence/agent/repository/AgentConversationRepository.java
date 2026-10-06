package com.ar.crm2.adapter.out.persistence.agent.repository;

import com.ar.crm2.adapter.out.persistence.agent.entity.AgentConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentConversationRepository extends JpaRepository<AgentConversationEntity, String> {

    Optional<AgentConversationEntity> findByOwnerId(String ownerId);
}
