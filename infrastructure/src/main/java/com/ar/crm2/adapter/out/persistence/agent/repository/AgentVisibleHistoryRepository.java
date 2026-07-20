package com.ar.crm2.adapter.out.persistence.agent.repository;

import com.ar.crm2.adapter.out.persistence.agent.entity.AgentVisibleHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentVisibleHistoryRepository extends JpaRepository<AgentVisibleHistoryEntity, String> {
}
