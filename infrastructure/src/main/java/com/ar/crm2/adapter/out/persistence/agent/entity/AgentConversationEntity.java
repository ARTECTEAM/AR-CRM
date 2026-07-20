package com.ar.crm2.adapter.out.persistence.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "agent_conversations",
    uniqueConstraints = @UniqueConstraint(name = "uk_agent_conversation_owner", columnNames = "owner_id")
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AgentConversationEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
