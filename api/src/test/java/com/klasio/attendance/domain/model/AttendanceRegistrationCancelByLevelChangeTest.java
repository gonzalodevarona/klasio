package com.klasio.attendance.domain.model;

import com.klasio.attendance.domain.event.RegistrationCancelledByLevelChange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AttendanceRegistrationCancelByLevelChangeTest {

    private AttendanceRegistration registered() {
        return AttendanceRegistration.register(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "OPEN", 1, 60,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0),
                UUID.randomUUID());
    }

    private static void clear(AttendanceRegistration r) { r.clearDomainEvents(); }

    @Test
    void cancelByLevelChangeFromRegisteredTransitionsToCancelledBySystem() {
        AttendanceRegistration r = registered();
        clear(r);
        UUID actor = UUID.randomUUID();
        Instant now = Instant.now();

        r.cancelByLevelChange(actor, now, "OPEN", "BEGINNER");

        assertThat(r.getStatus()).isEqualTo(AttendanceRegistrationStatus.CANCELLED_BY_SYSTEM);
        assertThat(r.getCancelledAt()).isEqualTo(now);
        assertThat(r.getCancelledBy()).isEqualTo(actor);
        assertThat(r.getUpdatedAt()).isEqualTo(now);
        assertThat(r.getUpdatedBy()).isEqualTo(actor);
    }

    @Test
    void cancelByLevelChangeEmitsExactlyOneRegistrationCancelledByLevelChangeEvent() {
        AttendanceRegistration r = registered();
        clear(r);
        UUID actor = UUID.randomUUID();
        Instant now = Instant.now();

        r.cancelByLevelChange(actor, now, "OPEN", "BEGINNER");

        assertThat(r.getDomainEvents()).hasSize(1);
        assertThat(r.getDomainEvents().get(0)).isInstanceOf(RegistrationCancelledByLevelChange.class);
    }

    @Test
    void cancelByLevelChangeEventCarriesCorrectLevelFields() {
        AttendanceRegistration r = registered();
        clear(r);
        UUID actor = UUID.randomUUID();
        Instant now = Instant.now();

        r.cancelByLevelChange(actor, now, "OPEN", "BEGINNER");

        RegistrationCancelledByLevelChange event =
                (RegistrationCancelledByLevelChange) r.getDomainEvents().get(0);
        assertThat(event.previousClassLevel()).isEqualTo("OPEN");
        assertThat(event.newClassLevel()).isEqualTo("BEGINNER");
        assertThat(event.actorId()).isEqualTo(actor);
        assertThat(event.occurredAt()).isEqualTo(now);
    }

    @Test
    void cancelByLevelChangeFromCancelledByStudentThrowsIllegalState() {
        AttendanceRegistration r = registered();
        r.cancelByStudent(UUID.randomUUID(), Instant.now());

        assertThatThrownBy(() -> r.cancelByLevelChange(UUID.randomUUID(), Instant.now(), "OPEN", "BEGINNER"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel by level change");
    }

    @Test
    void cancelByLevelChangeFromCancelledBySystemThrowsIllegalState() {
        AttendanceRegistration r = registered();
        r.cancelBySystem(UUID.randomUUID(), Instant.now());

        assertThatThrownBy(() -> r.cancelByLevelChange(UUID.randomUUID(), Instant.now(), "OPEN", "BEGINNER"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel by level change");
    }

    @Test
    void cancelByLevelChangeFromSessionCancelledThrowsIllegalState() {
        AttendanceRegistration r = registered();
        r.cancelBySession(UUID.randomUUID(), Instant.now(), null);

        assertThatThrownBy(() -> r.cancelByLevelChange(UUID.randomUUID(), Instant.now(), "OPEN", "BEGINNER"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel by level change");
    }

    @Test
    void cancelByLevelChangeFromPresentThrowsIllegalState() {
        AttendanceRegistration r = registered();
        r.markPresent(UUID.randomUUID(), Instant.now());

        assertThatThrownBy(() -> r.cancelByLevelChange(UUID.randomUUID(), Instant.now(), "OPEN", "BEGINNER"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel by level change");
    }

    @Test
    void cancelByLevelChangeRejectsNullActorId() {
        AttendanceRegistration r = registered();

        assertThatThrownBy(() -> r.cancelByLevelChange(null, Instant.now(), "OPEN", "BEGINNER"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void cancelByLevelChangeRejectsNullNow() {
        AttendanceRegistration r = registered();

        assertThatThrownBy(() -> r.cancelByLevelChange(UUID.randomUUID(), null, "OPEN", "BEGINNER"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void cancelByLevelChangeRejectsNullPreviousLevel() {
        AttendanceRegistration r = registered();

        assertThatThrownBy(() -> r.cancelByLevelChange(UUID.randomUUID(), Instant.now(), null, "BEGINNER"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void cancelByLevelChangeRejectsNullNewLevel() {
        AttendanceRegistration r = registered();

        assertThatThrownBy(() -> r.cancelByLevelChange(UUID.randomUUID(), Instant.now(), "OPEN", null))
                .isInstanceOf(NullPointerException.class);
    }
}
