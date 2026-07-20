package com.ar.crm2.adapter.out.persistence.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "agent_turn_requests",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_agent_turn_request_owner_key",
        columnNames = {"owner_id", "idempotency_key"}
    )
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AgentTurnRequestEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "fingerprint", nullable = false)
    private String fingerprint;

    @OneToOne(optional = false)
    @JoinColumn(name = "turn_id", nullable = false, unique = true)
    private AgentTurnEntity turn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
