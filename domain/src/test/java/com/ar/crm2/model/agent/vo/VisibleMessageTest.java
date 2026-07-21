package com.ar.crm2.model.agent.vo;

import com.ar.crm2.exception.InvariantViolationException;
import com.ar.crm2.model.agent.enums.VisibleMessageRole;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VisibleMessageTest {

    @Test
    void vocabulary_providesExactlyTheTwoSpeakerRoles() {
        assertThat(VisibleMessageRole.values())
                .containsExactly(VisibleMessageRole.USER, VisibleMessageRole.ASSISTANT);
    }

    @Test
    void vocabulary_isNotPubliclyInstantiableAndUsesUppercaseSpeakerNames() {
        assertThat(Modifier.isFinal(VisibleMessageRole.class.getModifiers())).isTrue();
        for (java.lang.reflect.Constructor<?> constructor : VisibleMessageRole.class.getDeclaredConstructors()) {
            assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        }
        assertThat(VisibleMessageRole.USER.name()).isEqualTo("USER");
        assertThat(VisibleMessageRole.ASSISTANT.name()).isEqualTo("ASSISTANT");
    }

    @Test
    void storage_form_roundTripsForBothSpeakerRoles() {
        for (VisibleMessageRole role : VisibleMessageRole.values()) {
            assertThat(VisibleMessageRole.fromStorage(role.name()))
                    .isEqualTo(role);
        }
    }

    @Test
    void storage_form_rejectsBlankNullAndUnknownSpeakerRole() {
        assertThatThrownBy(() -> VisibleMessageRole.fromStorage(null))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> VisibleMessageRole.fromStorage("  "))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> VisibleMessageRole.fromStorage("SYSTEM"))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> VisibleMessageRole.fromStorage("tool"))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void storage_form_isClosedAndRejectsAnyAttemptToIntroduceNewRoles() {
        assertThat(VisibleMessageRole.class.getEnumConstants())
                .containsExactly(VisibleMessageRole.USER, VisibleMessageRole.ASSISTANT);
    }

    @Test
    void buildsUserAndAssistantVisibleMessagesWithTrimmedContent() {
        VisibleMessage user = VisibleMessage.user("  question from owner ");
        VisibleMessage assistant = VisibleMessage.assistant(" canonical answer ");

        assertThat(user.role()).isEqualTo(VisibleMessageRole.USER);
        assertThat(user.content()).isEqualTo("question from owner");
        assertThat(assistant.role()).isEqualTo(VisibleMessageRole.ASSISTANT);
        assertThat(assistant.content()).isEqualTo("canonical answer");
    }

    @Test
    void canonicalFactoryTrimsContentAndRejectsBlankOrMissingInputs() {
        assertThat(VisibleMessage.of(VisibleMessageRole.USER, "  trimmed content  "))
                .isEqualTo(VisibleMessage.user("trimmed content"));

        assertThatThrownBy(() -> VisibleMessage.of(null, "content"))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> VisibleMessage.of(VisibleMessageRole.USER, null))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> VisibleMessage.of(VisibleMessageRole.USER, "   "))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> VisibleMessage.of(VisibleMessageRole.USER, ""))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void visibleMessageIsAnImmutableSpeakerProvenanceAndExcludesAuthorizationFields() {
        VisibleMessage message = VisibleMessage.user("hello");

        assertThat(message.role()).isEqualTo(VisibleMessageRole.USER);
        assertThat(message.content()).isEqualTo("hello");
        assertThat(message).isEqualTo(VisibleMessage.user("hello"))
                .isNotEqualTo(VisibleMessage.assistant("hello"));
        assertThat(message).isNotEqualTo(VisibleMessage.user("different"));
    }

    @Test
    void userAndAssistantFactoriesRejectBlankContent() {
        assertThatThrownBy(() -> VisibleMessage.user(null))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> VisibleMessage.user("   "))
                .isInstanceOf(InvariantViolationException.class);
        assertThatThrownBy(() -> VisibleMessage.assistant(""))
                .isInstanceOf(InvariantViolationException.class);
    }
}
