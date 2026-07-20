package com.ar.crm2.adapter.out.persistence.agent.repository;

import com.ar.crm2.adapter.out.persistence.agent.entity.AgentTurnEntity;
import com.ar.crm2.model.agent.enums.TurnState;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AgentTurnRepository extends JpaRepository<AgentTurnEntity, String> {

    Optional<AgentTurnEntity> findByIdAndConversationOwnerId(String id, String ownerId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update AgentTurnEntity turn
            set turn.state = :completed, turn.updatedAt = :updatedAt
            where turn.id = :turnId
              and turn.conversation.ownerId = :ownerId
              and turn.state = :prepared
            """)
    int transitionPreparedToCompleted(
            @Param("turnId") String turnId,
            @Param("ownerId") String ownerId,
            @Param("prepared") TurnState prepared,
            @Param("completed") TurnState completed,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
