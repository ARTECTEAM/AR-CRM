package com.ar.crm2.model.agent.tool.entity;

import com.ar.crm2.exception.InvariantViolationException;
import com.ar.crm2.model.agent.tool.enums.AgentToolActionStatus;
import com.ar.crm2.model.agent.tool.vo.AgentToolActionId;
import com.ar.crm2.model.agent.tool.vo.AgentToolName;
import com.ar.crm2.model.agent.tool.vo.AgentToolResource;
import com.ar.crm2.model.agent.vo.AgentOwnerId;
import com.ar.crm2.model.agent.vo.TurnId;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fixed timestamps used by PR9a Domain tests. Centralising them here
 * guarantees every test that asserts a {@code completedAt} / {@code createdAt}
 * is deterministic and independent of the wall clock.
 */
final class LocalTimeFixtures {

    static final LocalDateTime PERSISTED_CREATED_AT = LocalDateTime.parse("2026-07-21T09:00:00");
    static final Clock CLOCK = Clock.fixed(PERSISTED_CREATED_AT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    static final LocalDateTime FIRST_COMPLETED_AT = LocalDateTime.parse("2026-07-21T09:05:00");
    static final LocalDateTime SECOND_COMPLETED_AT = LocalDateTime.parse("2026-07-21T09:10:00");

    private LocalTimeFixtures() {
    }
}

/**
 * Strict-TDD coverage for the PR9a owner-scoped agent tool action ledger.
 *
 * <p>Each test exercises production behaviour end-to-end: deterministic
 * identity, the PENDING → COMPLETED immutable lifecycle, the
 * first-resource-wins replay contract, the strict reconstitution invariants
 * for both states, the redacted {@code toString} contract, and the
 * controlled rejection paths. Timestamps are fixed values from
 * {@link LocalTimeFixtures} so the tests do not depend on the wall clock.
 */
class AgentToolActionTest {

    private static final AgentOwnerId OWNER = AgentOwnerId.from("user-42");
    private static final AgentOwnerId OTHER_OWNER = AgentOwnerId.from("user-99");
    private static final TurnId TURN = TurnId.from(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final TurnId OTHER_TURN = TurnId.from(UUID.fromString("99999999-9999-9999-9999-999999999999"));
    private static final String CANONICAL = "create_contact|empresa=acme";
    private static final String OTHER_CANONICAL = "create_contact|empresa=other";

    @Test
    void entityClassIsNotFinalAndUsesIdentityBasedEquality() {
        assertThat(Modifier.isFinal(AgentToolAction.class.getModifiers()))
                .as("domain entities must not be declared final")
                .isFalse();

        AgentToolAction first = AgentToolAction.createPending(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL, LocalTimeFixtures.CLOCK);
        AgentToolAction second = AgentToolAction.createPending(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL, LocalTimeFixtures.CLOCK);

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    void createPendingGeneratesADeterministicSha256IdFromTrustedInputs() {
        AgentToolAction first = AgentToolAction.createPending(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL, LocalTimeFixtures.CLOCK);
        AgentToolAction second = AgentToolAction.createPending(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL, LocalTimeFixtures.CLOCK);

        assertThat(first.getId())
                .as("same owner + turn + tool + canonical arguments must produce the same id")
                .isEqualTo(second.getId());
        assertThat(first.getId().value())
                .as("the id must be the SHA-256 hex of (owner|turn|tool|canonical)")
                .hasSize(64)
                .matches("^[0-9a-f]{64}$");
    }

    @Test
    void actionIdAcceptsOnlyCanonicalLowercaseSha256Hex() {
        assertThat(new AgentToolActionId("a".repeat(64)).value()).isEqualTo("a".repeat(64));
        assertThatThrownBy(() -> new AgentToolActionId(null))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> new AgentToolActionId(" "))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> new AgentToolActionId("a".repeat(63)))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> new AgentToolActionId("a".repeat(65)))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> new AgentToolActionId("A".repeat(64)))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> new AgentToolActionId("g".repeat(64)))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void createPendingChangesTheIdWhenAnyTrustedInputDiffers() {
        AgentToolActionId fromOwner = AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL);
        AgentToolActionId fromDifferentOwner = AgentToolActionId.derive(OTHER_OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL);

        AgentToolActionId fromTurn = AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL);
        AgentToolActionId fromDifferentTurn = AgentToolActionId.derive(OWNER, OTHER_TURN, AgentToolName.CREATE_CONTACT, CANONICAL);

        AgentToolActionId fromTool = AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL);
        AgentToolActionId fromDifferentTool = AgentToolActionId.derive(OWNER, TURN, AgentToolName.FIND_CONTACTS, CANONICAL);

        AgentToolActionId fromArgs = AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL);
        AgentToolActionId fromDifferentArgs = AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, OTHER_CANONICAL);

        assertThat(fromOwner).isNotEqualTo(fromDifferentOwner);
        assertThat(fromTurn).isNotEqualTo(fromDifferentTurn);
        assertThat(fromTool).isNotEqualTo(fromDifferentTool);
        assertThat(fromArgs).isNotEqualTo(fromDifferentArgs);
    }

    @Test
    void createPendingProducesAPendingActionAtTheTrustedFixedClock() {
        AgentToolAction pending = AgentToolAction.createPending(
                OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL, LocalTimeFixtures.CLOCK);

        assertThat(pending.getOwnerId()).isEqualTo(OWNER);
        assertThat(pending.getTurnId()).isEqualTo(TURN);
        assertThat(pending.getToolName()).isEqualTo(AgentToolName.CREATE_CONTACT);
        assertThat(pending.getCanonicalArguments()).isEqualTo(CANONICAL);
        assertThat(pending.getStatus()).isEqualTo(AgentToolActionStatus.PENDING);
        assertThat(pending.getResource()).isNull();
        assertThat(pending.getCompletedAt()).isNull();
        assertThat(pending.getCreatedAt())
                .as("the factory must derive createdAt internally from the trusted clock")
                .isEqualTo(LocalTimeFixtures.PERSISTED_CREATED_AT);
        assertThat(pending.getId())
                .as("the id must be derived from the four trusted inputs and never supplied by the caller")
                .isEqualTo(AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL));
    }

    @Test
    void createPendingRejectsNullOrBlankInputs() {
        assertThatThrownBy(() -> AgentToolAction.createPending(
                null, TURN, AgentToolName.CREATE_CONTACT, CANONICAL, LocalTimeFixtures.CLOCK))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> AgentToolAction.createPending(
                OWNER, null, AgentToolName.CREATE_CONTACT, CANONICAL, LocalTimeFixtures.CLOCK))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> AgentToolAction.createPending(
                OWNER, TURN, null, CANONICAL, LocalTimeFixtures.CLOCK))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> AgentToolAction.createPending(
                OWNER, TURN, AgentToolName.CREATE_CONTACT, " ", LocalTimeFixtures.CLOCK))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> AgentToolAction.createPending(
                OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL, null))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void completeWithReturnsACompletedInstancePreservingIdentityAndCanonicalArguments() {
        AgentToolAction pending = AgentToolAction.createPending(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL, LocalTimeFixtures.CLOCK);
        AgentToolResource resource = new AgentToolResource("contacto", "contacto-001");
        LocalDateTime completedAt = LocalTimeFixtures.FIRST_COMPLETED_AT;

        AgentToolAction completed = pending.completeWith(resource, completedAt);

        assertThat(completed.getStatus()).isEqualTo(AgentToolActionStatus.COMPLETED);
        assertThat(completed.getResource()).isEqualTo(resource);
        assertThat(completed.getCompletedAt()).isEqualTo(completedAt);
        assertThat(completed.getId())
                .as("completion must preserve the deterministic identity")
                .isEqualTo(pending.getId());
        assertThat(completed.getOwnerId()).isEqualTo(pending.getOwnerId());
        assertThat(completed.getTurnId()).isEqualTo(pending.getTurnId());
        assertThat(completed.getToolName()).isEqualTo(pending.getToolName());
        assertThat(completed.getCanonicalArguments()).isEqualTo(pending.getCanonicalArguments());
        assertThat(completed.getCreatedAt())
                .as("creation timestamp must not change on completion")
                .isEqualTo(pending.getCreatedAt());
        assertThat(pending.getStatus())
                .as("the source PENDING instance must remain untouched")
                .isEqualTo(AgentToolActionStatus.PENDING);
        assertThat(pending.getResource())
                .as("the source PENDING instance must remain untouched")
                .isNull();
        assertThat(pending.getCompletedAt())
                .as("the source PENDING instance must remain untouched")
                .isNull();
    }

    @Test
    void completeWithRejectsNullResourceOrNullCompletedAt() {
        AgentToolAction pending = AgentToolAction.createPending(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL, LocalTimeFixtures.CLOCK);

        assertThatThrownBy(() -> pending.completeWith(null, LocalTimeFixtures.FIRST_COMPLETED_AT))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> pending.completeWith(
                new AgentToolResource("contacto", "contacto-001"), null))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void firstCompletionRejectsReversedChronologyButAllowsEquality() {
        AgentToolActionId id = AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL);
        AgentToolAction pending = AgentToolAction.reconstitute(
                id, OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL,
                AgentToolActionStatus.PENDING, null,
                LocalTimeFixtures.PERSISTED_CREATED_AT, null);
        AgentToolResource resource = new AgentToolResource("contacto", "contacto-001");

        assertThatThrownBy(() -> pending.completeWith(
                resource, LocalTimeFixtures.PERSISTED_CREATED_AT.minusNanos(1)))
                .isInstanceOf(InvariantViolationException.class);
        assertThat(pending.completeWith(resource, LocalTimeFixtures.PERSISTED_CREATED_AT).getCompletedAt())
                .isEqualTo(LocalTimeFixtures.PERSISTED_CREATED_AT);
    }

    @Test
    void completedReconstitutionRejectsReversedChronologyButAllowsEquality() {
        AgentToolActionId id = AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL);
        AgentToolResource resource = new AgentToolResource("contacto", "contacto-001");

        assertThatThrownBy(() -> AgentToolAction.reconstitute(
                id, OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL,
                AgentToolActionStatus.COMPLETED, resource, LocalTimeFixtures.PERSISTED_CREATED_AT,
                LocalTimeFixtures.PERSISTED_CREATED_AT.minusNanos(1)))
                .isInstanceOf(InvariantViolationException.class);
        assertThat(AgentToolAction.reconstitute(
                id, OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL,
                AgentToolActionStatus.COMPLETED, resource, LocalTimeFixtures.PERSISTED_CREATED_AT,
                LocalTimeFixtures.PERSISTED_CREATED_AT).getCompletedAt())
                .isEqualTo(LocalTimeFixtures.PERSISTED_CREATED_AT);
    }

    @Test
    void completeWithIsAnIdempotentReplayOnAnAlreadyCompletedAction() {
        AgentToolAction pending = AgentToolAction.createPending(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL, LocalTimeFixtures.CLOCK);
        AgentToolResource firstResource = new AgentToolResource("contacto", "contacto-001");
        AgentToolResource secondResource = new AgentToolResource("contacto", "contacto-002");

        AgentToolAction firstCompletion = pending.completeWith(firstResource, LocalTimeFixtures.FIRST_COMPLETED_AT);
        AgentToolAction replayedCompletion = firstCompletion.completeWith(
                secondResource, LocalTimeFixtures.SECOND_COMPLETED_AT);

        assertThat(replayedCompletion.getStatus()).isEqualTo(AgentToolActionStatus.COMPLETED);
        assertThat(replayedCompletion.getResource())
                .as("a second completion must keep the FIRST canonical resource, even when a different one is supplied")
                .isEqualTo(firstResource);
        assertThat(replayedCompletion.getCompletedAt())
                .as("a second completion must keep the FIRST completedAt, even when a different one is supplied")
                .isEqualTo(LocalTimeFixtures.FIRST_COMPLETED_AT);
        assertThat(replayedCompletion.getId()).isEqualTo(pending.getId());
        assertThat(replayedCompletion.getOwnerId()).isEqualTo(OWNER);
        assertThat(replayedCompletion.getTurnId()).isEqualTo(TURN);
        assertThat(replayedCompletion.getToolName()).isEqualTo(AgentToolName.CREATE_CONTACT);
        assertThat(replayedCompletion.getCanonicalArguments()).isEqualTo(CANONICAL);
        assertThat(replayedCompletion.getCreatedAt()).isEqualTo(firstCompletion.getCreatedAt());
    }

    @Test
    void completeWithIsIdempotentOnAReconstitutedCompletedAction() {
        AgentToolActionId id = AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL);
        AgentToolResource firstResource = new AgentToolResource("contacto", "contacto-001");
        AgentToolAction completed = AgentToolAction.reconstitute(
                id, OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL,
                AgentToolActionStatus.COMPLETED, firstResource,
                LocalTimeFixtures.PERSISTED_CREATED_AT, LocalTimeFixtures.FIRST_COMPLETED_AT);

        AgentToolResource bogusResource = new AgentToolResource("contacto", "contacto-002");
        AgentToolAction replayed = completed.completeWith(bogusResource, LocalTimeFixtures.SECOND_COMPLETED_AT);

        assertThat(replayed)
                .as("a replayed completion on an already COMPLETED action must return the same canonical row")
                .isSameAs(completed);
        assertThat(replayed.getResource())
                .as("the FIRST canonical resource must be preserved, never replaced by a replay")
                .isEqualTo(firstResource);
        assertThat(replayed.getCompletedAt())
                .as("the FIRST completedAt must be preserved, never replaced by a replay")
                .isEqualTo(LocalTimeFixtures.FIRST_COMPLETED_AT);
        assertThat(replayed.getId()).isEqualTo(id);
        assertThat(replayed.getStatus()).isEqualTo(AgentToolActionStatus.COMPLETED);
    }

    @Test
    void reconstituteRejectsAnIdForgedForOtherwiseValidIdentity() {
        AgentToolActionId forged = new AgentToolActionId("a".repeat(64));
        assertThat(forged)
                .isNotEqualTo(AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL));

        assertThatThrownBy(() -> AgentToolAction.reconstitute(
                forged, OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL,
                AgentToolActionStatus.PENDING, null,
                LocalTimeFixtures.PERSISTED_CREATED_AT, null))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void canonicalArgumentsAndResourceValuesAreTrimmedAfterValidation() {
        String paddedCanonical = "  " + CANONICAL + "  ";
        AgentToolActionId canonicalId = AgentToolActionId.derive(
                OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL);
        AgentToolAction pending = AgentToolAction.reconstitute(
                canonicalId, OWNER, TURN, AgentToolName.CREATE_CONTACT, paddedCanonical,
                AgentToolActionStatus.PENDING, null,
                LocalTimeFixtures.PERSISTED_CREATED_AT, null);
        AgentToolResource resource = new AgentToolResource(" contacto ", " contacto-001 ");

        assertThat(pending.getCanonicalArguments()).isEqualTo(CANONICAL);
        assertThat(AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, paddedCanonical))
                .isEqualTo(canonicalId);
        assertThat(resource.resourceType()).isEqualTo("contacto");
        assertThat(resource.resourceId()).isEqualTo("contacto-001");
    }

    @Test
    void reconstitutePreservesFullIdentityOwnerTurnToolCanonicalStatusCreatedAtResourceAndCompletedAtForPending() {
        AgentToolActionId id = AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL);

        AgentToolAction pending = AgentToolAction.reconstitute(
                id, OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL,
                AgentToolActionStatus.PENDING, null,
                LocalTimeFixtures.PERSISTED_CREATED_AT, null);

        assertThat(pending.getId()).isEqualTo(id);
        assertThat(pending.getOwnerId()).isEqualTo(OWNER);
        assertThat(pending.getTurnId()).isEqualTo(TURN);
        assertThat(pending.getToolName()).isEqualTo(AgentToolName.CREATE_CONTACT);
        assertThat(pending.getCanonicalArguments()).isEqualTo(CANONICAL);
        assertThat(pending.getStatus()).isEqualTo(AgentToolActionStatus.PENDING);
        assertThat(pending.getResource()).isNull();
        assertThat(pending.getCreatedAt()).isEqualTo(LocalTimeFixtures.PERSISTED_CREATED_AT);
        assertThat(pending.getCompletedAt()).isNull();
    }

    @Test
    void reconstitutePreservesFullIdentityOwnerTurnToolCanonicalStatusCreatedAtResourceAndCompletedAtForCompleted() {
        AgentToolActionId id = AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL);
        AgentToolResource resource = new AgentToolResource("contacto", "contacto-001");

        AgentToolAction completed = AgentToolAction.reconstitute(
                id, OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL,
                AgentToolActionStatus.COMPLETED, resource,
                LocalTimeFixtures.PERSISTED_CREATED_AT, LocalTimeFixtures.FIRST_COMPLETED_AT);

        assertThat(completed.getId()).isEqualTo(id);
        assertThat(completed.getOwnerId()).isEqualTo(OWNER);
        assertThat(completed.getTurnId()).isEqualTo(TURN);
        assertThat(completed.getToolName()).isEqualTo(AgentToolName.CREATE_CONTACT);
        assertThat(completed.getCanonicalArguments()).isEqualTo(CANONICAL);
        assertThat(completed.getStatus()).isEqualTo(AgentToolActionStatus.COMPLETED);
        assertThat(completed.getResource()).isEqualTo(resource);
        assertThat(completed.getCreatedAt()).isEqualTo(LocalTimeFixtures.PERSISTED_CREATED_AT);
        assertThat(completed.getCompletedAt()).isEqualTo(LocalTimeFixtures.FIRST_COMPLETED_AT);
    }

    @Test
    void reconstituteRejectsPendingStatusWithNonNullResourceOrCompletedAt() {
        AgentToolActionId id = AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL);
        AgentToolResource resource = new AgentToolResource("contacto", "contacto-001");

        assertThatThrownBy(() -> AgentToolAction.reconstitute(
                id, OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL,
                AgentToolActionStatus.PENDING, resource,
                LocalTimeFixtures.PERSISTED_CREATED_AT, null))
                .isInstanceOf(InvariantViolationException.class);

        assertThatThrownBy(() -> AgentToolAction.reconstitute(
                id, OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL,
                AgentToolActionStatus.PENDING, null,
                LocalTimeFixtures.PERSISTED_CREATED_AT, LocalTimeFixtures.FIRST_COMPLETED_AT))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void reconstituteRejectsCompletedStatusWithoutResourceOrCompletedAt() {
        AgentToolActionId id = AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL);
        AgentToolResource resource = new AgentToolResource("contacto", "contacto-001");

        assertThatThrownBy(() -> AgentToolAction.reconstitute(
                id, OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL,
                AgentToolActionStatus.COMPLETED, null,
                LocalTimeFixtures.PERSISTED_CREATED_AT, LocalTimeFixtures.FIRST_COMPLETED_AT))
                .isInstanceOf(InvariantViolationException.class);

        assertThatThrownBy(() -> AgentToolAction.reconstitute(
                id, OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL,
                AgentToolActionStatus.COMPLETED, resource,
                LocalTimeFixtures.PERSISTED_CREATED_AT, null))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void reconstituteRejectsNullOrBlankIdentityOwnerTurnToolArgumentsOrStatus() {
        AgentToolActionId id = AgentToolActionId.derive(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL);

        assertThatThrownBy(() -> AgentToolAction.reconstitute(
                null, OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL,
                AgentToolActionStatus.PENDING, null,
                LocalTimeFixtures.PERSISTED_CREATED_AT, null))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> AgentToolAction.reconstitute(
                id, null, TURN, AgentToolName.CREATE_CONTACT, CANONICAL,
                AgentToolActionStatus.PENDING, null,
                LocalTimeFixtures.PERSISTED_CREATED_AT, null))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> AgentToolAction.reconstitute(
                id, OWNER, null, AgentToolName.CREATE_CONTACT, CANONICAL,
                AgentToolActionStatus.PENDING, null,
                LocalTimeFixtures.PERSISTED_CREATED_AT, null))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> AgentToolAction.reconstitute(
                id, OWNER, TURN, null, CANONICAL,
                AgentToolActionStatus.PENDING, null,
                LocalTimeFixtures.PERSISTED_CREATED_AT, null))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> AgentToolAction.reconstitute(
                id, OWNER, TURN, AgentToolName.CREATE_CONTACT, " ",
                AgentToolActionStatus.PENDING, null,
                LocalTimeFixtures.PERSISTED_CREATED_AT, null))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> AgentToolAction.reconstitute(
                id, OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL,
                null, null,
                LocalTimeFixtures.PERSISTED_CREATED_AT, null))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> AgentToolAction.reconstitute(
                id, OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL,
                AgentToolActionStatus.PENDING, null,
                null, null))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void toStringIncludesOnlyExplicitlySafeToolNameAndStatus() {
        AgentToolAction pending = AgentToolAction.createPending(OWNER, TURN, AgentToolName.CREATE_CONTACT, CANONICAL, LocalTimeFixtures.CLOCK);
        AgentToolAction completed = pending.completeWith(
                new AgentToolResource("contacto", "contacto-001"), LocalTimeFixtures.FIRST_COMPLETED_AT);

        assertThat(pending.toString())
                .isEqualTo("AgentToolAction(toolName=CREATE_CONTACT, status=PENDING)");
        assertThat(completed.toString())
                .isEqualTo("AgentToolAction(toolName=CREATE_CONTACT, status=COMPLETED)");
    }
}
