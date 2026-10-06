package com.ar.crm2.adapter.out.persistence.agent.repository;

import com.ar.crm2.adapter.out.persistence.agent.entity.AgentVisibleHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AgentVisibleHistoryRepository extends JpaRepository<AgentVisibleHistoryEntity, String> {

    Optional<AgentVisibleHistoryEntity> findFirstByTurnIdAndRoleOrderByVisibleAtDesc(String turnId, String role);

    List<AgentVisibleHistoryEntity> findByConversationOwnerIdAndTurnStateAndTurnIdNotOrderByVisibleAtDesc(
            String ownerId,
            com.ar.crm2.model.agent.enums.TurnState state,
            String excludedTurnId,
            Pageable pageable
    );
}
