package com.ar.crm2.model.agent.entity;

import com.ar.crm2.exception.DurableMemoryLifecycleException;
import com.ar.crm2.exception.MemorySafetyViolationException;
import com.ar.crm2.model.agent.enums.DurableMemoryStatus;
import com.ar.crm2.model.agent.policy.MemorySafetyContext;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.MemoryId;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DurableMemoryTest {

    private static final AgentOwnerId OWNER = AgentOwnerId.from("user-42");

    @Test
    void memoryId_createsAndReconstitutesValidUuid() {
        UUID value = UUID.randomUUID();

        assertThat(MemoryId.create().value()).isNotNull();
        assertThat(MemoryId.from(value).value()).isEqualTo(value);
    }

    @Test
    void memoryId_rejectsNullUuid() {
        assertThatThrownBy(() -> MemoryId.from(null))
                .isInstanceOf(com.ar.crm2.exception.InvariantViolationException.class);
    }

    @Test
    void createsOwnerBoundEligibleMemoryFromExplicitSafeRequest() {
        DurableMemory memory = DurableMemory.create(OWNER, "Use Spanish for summaries", MemorySafetyContext.explicitSafe());

        assertThat(memory.getOwnerId()).isEqualTo(OWNER);
        assertThat(memory.getContent()).isEqualTo("Use Spanish for summaries");
        assertThat(memory.getStatus()).isEqualTo(DurableMemoryStatus.ACTIVE);
        assertThat(memory.isEligibleAt(LocalDateTime.now())).isTrue();
    }

    @Test
    void toString_doesNotExposeMemoryContentOrOwnerIdentity() {
        DurableMemory memory = DurableMemory.create(OWNER, "Use Spanish for summaries", MemorySafetyContext.explicitSafe());

        assertThat(memory.toString())
                .doesNotContain(memory.getContent())
                .doesNotContain(OWNER.value());
    }

    @Test
    void rejectsBlankContent() {
        assertThatThrownBy(() -> DurableMemory.create(OWNER, " ", MemorySafetyContext.explicitSafe()))
                .isInstanceOf(com.ar.crm2.exception.InvariantViolationException.class);
    }

    @Test
    void rejectsInferredAndSensitiveOrRawContentAccordingToSafetyContract() {
        assertThatThrownBy(() -> DurableMemory.create(OWNER, "Inferred preference", MemorySafetyContext.inferred()))
                .isInstanceOf(MemorySafetyViolationException.class);
        assertThatThrownBy(() -> DurableMemory.create(OWNER, "secret", MemorySafetyContext.secret()))
                .isInstanceOf(MemorySafetyViolationException.class);
        assertThatThrownBy(() -> DurableMemory.create(OWNER, "password", MemorySafetyContext.credential()))
                .isInstanceOf(MemorySafetyViolationException.class);
        assertThatThrownBy(() -> DurableMemory.create(OWNER, "token", MemorySafetyContext.authenticationMaterial()))
                .isInstanceOf(MemorySafetyViolationException.class);
        assertThatThrownBy(() -> DurableMemory.create(OWNER, "{tool: payload}", MemorySafetyContext.rawToolPayload()))
                .isInstanceOf(MemorySafetyViolationException.class);
    }

    @Test
    void excludesExpiredMemoryFromEligibility() {
        LocalDateTime now = LocalDateTime.now();
        DurableMemory expired = DurableMemory.reconstitute(
                MemoryId.from(UUID.randomUUID()), OWNER, "Use Spanish", DurableMemoryStatus.ACTIVE,
                now.minusDays(2), now.minusDays(2), now.minusDays(1), null, null, null
        );

        assertThat(expired.isEligibleAt(now)).isFalse();
    }

    @Test
    void supersedesOnlyExplicitOwnerBoundTargetAndKeepsOriginalImmutable() {
        DurableMemory original = DurableMemory.create(OWNER, "Use Spanish", MemorySafetyContext.explicitSafe());
        DurableMemory successor = DurableMemory.create(OWNER, "Use English", MemorySafetyContext.explicitSafe());

        DurableMemory superseded = original.supersedeWith(successor, OWNER, original.getId());

        assertThat(superseded.getStatus()).isEqualTo(DurableMemoryStatus.SUPERSEDED);
        assertThat(superseded.getSupersededById()).isEqualTo(successor.getId());
        assertThat(superseded.isEligibleAt(LocalDateTime.now())).isFalse();
        assertThat(original.getStatus()).isEqualTo(DurableMemoryStatus.ACTIVE);
    }

    @Test
    void rejectsSupersessionForWrongOwnerOrExplicitTarget() {
        DurableMemory original = DurableMemory.create(OWNER, "Use Spanish", MemorySafetyContext.explicitSafe());
        DurableMemory successor = DurableMemory.create(OWNER, "Use English", MemorySafetyContext.explicitSafe());

        assertThatThrownBy(() -> original.supersedeWith(successor, AgentOwnerId.from("user-99"), original.getId()))
                .isInstanceOf(DurableMemoryLifecycleException.class);
        assertThatThrownBy(() -> original.supersedeWith(successor, OWNER, MemoryId.from(UUID.randomUUID())))
                .isInstanceOf(DurableMemoryLifecycleException.class);
    }

    @Test
    void rejectsInactiveExpiredWrongOwnerOrSameIdSuccessors() {
        LocalDateTime now = LocalDateTime.now();
        DurableMemory original = DurableMemory.create(OWNER, "Use Spanish", MemorySafetyContext.explicitSafe());
        DurableMemory activeSuccessor = DurableMemory.create(OWNER, "Use English", MemorySafetyContext.explicitSafe());
        DurableMemory deleted = DurableMemory.reconstitute(MemoryId.from(UUID.randomUUID()), OWNER, "Deleted",
                DurableMemoryStatus.DELETED, now, now, null, null, null, now);
        DurableMemory superseded = DurableMemory.reconstitute(MemoryId.from(UUID.randomUUID()), OWNER, "Superseded",
                DurableMemoryStatus.SUPERSEDED, now, now, null, now, activeSuccessor.getId(), null);
        DurableMemory expired = DurableMemory.reconstitute(MemoryId.from(UUID.randomUUID()), OWNER, "Expired",
                DurableMemoryStatus.ACTIVE, now, now, now.minusSeconds(1), null, null, null);
        DurableMemory anotherOwner = DurableMemory.create(AgentOwnerId.from("user-99"), "Another owner", MemorySafetyContext.explicitSafe());
        DurableMemory sameId = DurableMemory.reconstitute(original.getId(), OWNER, "Same id", DurableMemoryStatus.ACTIVE,
                now, now, null, null, null, null);

        assertThatThrownBy(() -> original.supersedeWith(deleted, OWNER, original.getId()))
                .isInstanceOf(DurableMemoryLifecycleException.class);
        assertThatThrownBy(() -> original.supersedeWith(superseded, OWNER, original.getId()))
                .isInstanceOf(DurableMemoryLifecycleException.class);
        assertThatThrownBy(() -> original.supersedeWith(expired, OWNER, original.getId()))
                .isInstanceOf(DurableMemoryLifecycleException.class);
        assertThatThrownBy(() -> original.supersedeWith(anotherOwner, OWNER, original.getId()))
                .isInstanceOf(DurableMemoryLifecycleException.class);
        assertThatThrownBy(() -> original.supersedeWith(sameId, OWNER, original.getId()))
                .isInstanceOf(DurableMemoryLifecycleException.class);
    }

    @Test
    void deletesExplicitOwnerBoundTargetAndRejectsRepeatedTransitions() {
        DurableMemory memory = DurableMemory.create(OWNER, "Use Spanish", MemorySafetyContext.explicitSafe());

        DurableMemory deleted = memory.delete(OWNER, memory.getId());

        assertThat(deleted.getStatus()).isEqualTo(DurableMemoryStatus.DELETED);
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(deleted.isEligibleAt(LocalDateTime.now())).isFalse();
        assertThatThrownBy(() -> deleted.delete(OWNER, deleted.getId()))
                .isInstanceOf(DurableMemoryLifecycleException.class);
        assertThatThrownBy(() -> deleted.supersedeWith(memory, OWNER, deleted.getId()))
                .isInstanceOf(DurableMemoryLifecycleException.class);
    }

    @Test
    void reconstitutionRejectsImpossibleLifecycleCombinations() {
        LocalDateTime now = LocalDateTime.now();

        assertThatThrownBy(() -> DurableMemory.reconstitute(
                MemoryId.from(UUID.randomUUID()), OWNER, "Use Spanish", DurableMemoryStatus.SUPERSEDED,
                now, now, null, null, null, null
        )).isInstanceOf(com.ar.crm2.exception.InvariantViolationException.class);
        assertThatThrownBy(() -> DurableMemory.reconstitute(
                MemoryId.from(UUID.randomUUID()), OWNER, "Use Spanish", DurableMemoryStatus.DELETED,
                now, now, null, null, null, null
        )).isInstanceOf(com.ar.crm2.exception.InvariantViolationException.class);
        MemoryId id = MemoryId.from(UUID.randomUUID());
        assertThatThrownBy(() -> DurableMemory.reconstitute(
                id, OWNER, "Use Spanish", DurableMemoryStatus.SUPERSEDED,
                now, now, null, now, id, null
        )).isInstanceOf(com.ar.crm2.exception.InvariantViolationException.class);
    }

    @Test
    void entitiesUseIdentityEquality() {
        MemoryId id = MemoryId.from(UUID.randomUUID());
        LocalDateTime now = LocalDateTime.now();
        DurableMemory active = DurableMemory.reconstitute(id, OWNER, "Use Spanish", DurableMemoryStatus.ACTIVE,
                now, now, null, null, null, null);
        DurableMemory deleted = DurableMemory.reconstitute(id, OWNER, "Use English", DurableMemoryStatus.DELETED,
                now, now, null, null, null, now);

        assertThat(Modifier.isFinal(DurableMemory.class.getModifiers())).isFalse();
        assertThat(active).isEqualTo(deleted).hasSameHashCodeAs(deleted);
    }
}
