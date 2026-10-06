package com.ar.crm2.adapter.out.persistence.agent.tool;

import com.ar.crm2.model.agent.tool.enums.AgentToolActionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_tool_actions", indexes = {
        @Index(name = "idx_agent_tool_action_owner_turn_tool", columnList = "owner_id,turn_id,tool_name"),
        @Index(name = "idx_agent_tool_action_owner_status", columnList = "owner_id,status")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentToolActionEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "owner_id", length = 100, nullable = false)
    private String ownerId;

    @Column(name = "turn_id", length = 36, nullable = false)
    private String turnId;

    @Column(name = "tool_name", length = 40, nullable = false)
    private String toolName;

    @Column(name = "canonical_arguments", length = 2000, nullable = false)
    private String canonicalArguments;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AgentToolActionStatus status;

    @Column(name = "resource_type", length = 40)
    private String resourceType;

    @Column(name = "resource_id", length = 64)
    private String resourceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
