package com.ar.crm2.adapter.out.persistence.agent.memory;

import com.ar.crm2.model.agent.enums.DurableMemoryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_durable_memories", indexes = {
        @Index(name = "idx_agent_memory_owner_status_expiry_order", columnList = "owner_id,status,expires_at,created_at,id"),
        @Index(name = "idx_agent_memory_retention", columnList = "expires_at,deleted_at")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DurableMemoryEntity {
    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DurableMemoryStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "superseded_at")
    private LocalDateTime supersededAt;

    @Column(name = "superseded_by_id", length = 36)
    private String supersededById;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
