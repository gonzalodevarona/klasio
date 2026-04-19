package com.klasio.attendance.domain.model;

import com.klasio.attendance.domain.event.RegistrationCancelledBySession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AttendanceRegistrationCancelBySessionTest {

    private AttendanceRegistration registered() {
        return AttendanceRegistration.register(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "BEGINNER", 1, 60,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0),
                UUID.randomUUID());
    }

    private static void clear(AttendanceRegistration r) { r.clearDomainEvents(); }

    @Test
    void cancelBySessionFromRegisteredTransitionsToSessionCancelled() {
        AttendanceRegistration r = registered();
        clear(r);
        UUID actor = UUID.randomUUID();
        r.cancelBySession(actor, Instant.now());
        assertThat(r.getStatus()).isEqualTo(AttendanceRegistrationStatus.SESSION_CANCELLED);
        assertThat(r.getDomainEvents()).hasSize(1).first().isInstanceOf(RegistrationCancelledBySession.class);
        RegistrationCancelledBySession e = (RegistrationCancelledBySession) r.getDomainEvents().get(0);
        assertThat(e.priorStatus()).isEqualTo(AttendanceRegistrationStatus.REGISTERED);
    }

    @Test
    void cancelBySessionFromPresentTransitionsAndPreservesPriorStatus() {
        AttendanceRegistration r = registered();
        r.markPresent(UUID.randomUUID(), Instant.now());
        clear(r);
        r.cancelBySession(UUID.randomUUID(), Instant.now());
        assertThat(r.getStatus()).isEqualTo(AttendanceRegistrationStatus.SESSION_CANCELLED);
        RegistrationCancelledBySession e = (RegistrationCancelledBySession) r.getDomainEvents().get(0);
        assertThat(e.priorStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
    }

    @Test
    void cancelBySessionFromPresentNoHoursPreservesPriorStatus() {
        AttendanceRegistration r = registered();
        r.markPresentNoHours(UUID.randomUUID(), Instant.now());
        clear(r);
        r.cancelBySession(UUID.randomUUID(), Instant.now());
        RegistrationCancelledBySession e = (RegistrationCancelledBySession) r.getDomainEvents().get(0);
        assertThat(e.priorStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT_NO_HOURS);
    }

    @Test
    void cancelBySessionFromAbsentPreservesPriorStatus() {
        AttendanceRegistration r = registered();
        r.markAbsent(UUID.randomUUID(), Instant.now());
        clear(r);
        r.cancelBySession(UUID.randomUUID(), Instant.now());
        RegistrationCancelledBySession e = (RegistrationCancelledBySession) r.getDomainEvents().get(0);
        assertThat(e.priorStatus()).isEqualTo(AttendanceRegistrationStatus.ABSENT);
    }

    @Test
    void cancelBySessionIsIdempotentWhenAlreadySessionCancelled() {
        AttendanceRegistration r = registered();
        r.cancelBySession(UUID.randomUUID(), Instant.now());
        clear(r);
        r.cancelBySession(UUID.randomUUID(), Instant.now());
        assertThat(r.getDomainEvents()).isEmpty();
        assertThat(r.getStatus()).isEqualTo(AttendanceRegistrationStatus.SESSION_CANCELLED);
    }

    @Test
    void cancelBySessionRejectsFromCancelledByStudent() {
        AttendanceRegistration r = registered();
        r.cancelByStudent(UUID.randomUUID(), Instant.now());
        assertThatThrownBy(() -> r.cancelBySession(UUID.randomUUID(), Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelBySessionRejectsFromCancelledBySystem() {
        AttendanceRegistration r = registered();
        r.cancelBySystem(UUID.randomUUID(), Instant.now());
        assertThatThrownBy(() -> r.cancelBySession(UUID.randomUUID(), Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }
}
