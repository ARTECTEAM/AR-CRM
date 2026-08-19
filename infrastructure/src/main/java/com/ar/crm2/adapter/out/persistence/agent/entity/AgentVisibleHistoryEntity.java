package com.ar.crm2.adapter.out.persistence.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_visible_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AgentVisibleHistoryEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AgentConversationEntity conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turn_id", nullable = false)
    private AgentTurnEntity turn;

    @Column(name = "role", length = 20, nullable = false)
    private String role;

    @Column(name = "content", length = 4096, nullable = false)
    private String content;

    @Column(name = "visible_at", nullable = false)
    private LocalDateTime visibleAt;
}
