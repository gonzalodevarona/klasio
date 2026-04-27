package com.klasio.attendance.domain.model;

import com.klasio.attendance.domain.event.AttendanceMarkedAbsent;
import com.klasio.attendance.domain.event.AttendanceMarkedPresent;
import com.klasio.attendance.domain.event.AttendanceMarkedPresentNoHours;
import com.klasio.attendance.domain.event.AttendanceCorrected;
import com.klasio.attendance.domain.event.AttendanceRegistered;
import com.klasio.attendance.domain.event.RegistrationCancelled;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AttendanceRegistrationTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();
    private static final String LEVEL = "BEGINNER";
    private static final LocalDate DATE = LocalDate.of(2026, 5, 4);
    private static final LocalTime START = LocalTime.of(18, 0);
    private static final LocalTime END = LocalTime.of(19, 0);
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Test
    void register_validArgs_createsRegisteredStatus() {
        AttendanceRegistration reg = AttendanceRegistration.register(
                SESSION_ID, TENANT_ID, CLASS_ID, STUDENT_ID, ENROLLMENT_ID, MEMBERSHIP_ID,
                LEVEL, 1, 60, DATE, START, END, ACTOR_ID
        );

        assertThat(reg.getId()).isNotNull();
        assertThat(reg.getStatus()).isEqualTo(AttendanceRegistrationStatus.REGISTERED);
        assertThat(reg.getIntendedHours()).isEqualTo(1);
        assertThat(reg.getLevelAtRegistration()).isEqualTo(LEVEL);
        assertThat(reg.getSessionDate()).isEqualTo(DATE);
        assertThat(reg.getCreatedAt()).isNotNull();
    }

    @Test
    void register_emitsDomainEvent() {
        AttendanceRegistration reg = AttendanceRegistration.register(
                SESSION_ID, TENANT_ID, CLASS_ID, STUDENT_ID, ENROLLMENT_ID, MEMBERSHIP_ID,
                LEVEL, 1, 60, DATE, START, END, ACTOR_ID
        );

        assertThat(reg.getDomainEvents()).hasSize(1);
        assertThat(reg.getDomainEvents().get(0)).isInstanceOf(AttendanceRegistered.class);

        AttendanceRegistered event = (AttendanceRegistered) reg.getDomainEvents().get(0);
        assertThat(event.studentId()).isEqualTo(STUDENT_ID);
        assertThat(event.classId()).isEqualTo(CLASS_ID);
        assertThat(event.intendedHours()).isEqualTo(1);
    }

    @Test
    void register_classDurationUnder60Minutes_throws() {
        assertThatThrownBy(() -> AttendanceRegistration.register(
                SESSION_ID, TENANT_ID, CLASS_ID, STUDENT_ID, ENROLLMENT_ID, MEMBERSHIP_ID,
                LEVEL, 1, 45, DATE, START, END, ACTOR_ID
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 60 minutes");
    }

    @Test
    void register_intendedHoursZero_throws() {
        assertThatThrownBy(() -> AttendanceRegistration.register(
                SESSION_ID, TENANT_ID, CLASS_ID, STUDENT_ID, ENROLLMENT_ID, MEMBERSHIP_ID,
                LEVEL, 0, 60, DATE, START, END, ACTOR_ID
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intendedHours must be between");
    }

    @Test
    void register_intendedHoursExceedsFloor_throws() {
        // 60 min → floor = 1 hour max; requesting 2 should fail
        assertThatThrownBy(() -> AttendanceRegistration.register(
                SESSION_ID, TENANT_ID, CLASS_ID, STUDENT_ID, ENROLLMENT_ID, MEMBERSHIP_ID,
                LEVEL, 2, 60, DATE, START, END, ACTOR_ID
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intendedHours must be between");
    }

    @Test
    void register_90minClass_allows1Hour() {
        // 90 min → floor = 1; requesting 1 is valid
        AttendanceRegistration reg = AttendanceRegistration.register(
                SESSION_ID, TENANT_ID, CLASS_ID, STUDENT_ID, ENROLLMENT_ID, MEMBERSHIP_ID,
                LEVEL, 1, 90, DATE, START, END, ACTOR_ID
        );
        assertThat(reg.getIntendedHours()).isEqualTo(1);
    }

    @Test
    void register_120minClass_allows2Hours() {
        // 120 min → floor = 2
        AttendanceRegistration reg = AttendanceRegistration.register(
                SESSION_ID, TENANT_ID, CLASS_ID, STUDENT_ID, ENROLLMENT_ID, MEMBERSHIP_ID,
                LEVEL, 2, 120, DATE, START, END, ACTOR_ID
        );
        assertThat(reg.getIntendedHours()).isEqualTo(2);
    }

    @Test
    void register_nullArgs_throws() {
        assertThatThrownBy(() -> AttendanceRegistration.register(
                null, TENANT_ID, CLASS_ID, STUDENT_ID, ENROLLMENT_ID, MEMBERSHIP_ID,
                LEVEL, 1, 60, DATE, START, END, ACTOR_ID
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void clearDomainEvents_removesEvents() {
        AttendanceRegistration reg = AttendanceRegistration.register(
                SESSION_ID, TENANT_ID, CLASS_ID, STUDENT_ID, ENROLLMENT_ID, MEMBERSHIP_ID,
                LEVEL, 1, 60, DATE, START, END, ACTOR_ID
        );
        reg.clearDomainEvents();
        assertThat(reg.getDomainEvents()).isEmpty();
    }

    // ---------------------------------------------------------------
    // cancelByStudent()
    // ---------------------------------------------------------------

    @Test
    void cancelByStudent_onRegistered_transitionsStatusAndPopulatesFields() {
        AttendanceRegistration reg = AttendanceRegistration.register(
                SESSION_ID, TENANT_ID, CLASS_ID, STUDENT_ID, ENROLLMENT_ID, MEMBERSHIP_ID,
                LEVEL, 1, 60, DATE, START, END, ACTOR_ID
        );
        reg.clearDomainEvents();
        Instant now = Instant.now();
        UUID cancelActor = UUID.randomUUID();

        reg.cancelByStudent(cancelActor, now);

        assertThat(reg.getStatus()).isEqualTo(AttendanceRegistrationStatus.CANCELLED_BY_STUDENT);
        assertThat(reg.getCancelledAt()).isEqualTo(now);
        assertThat(reg.getCancelledBy()).isEqualTo(cancelActor);
        assertThat(reg.getUpdatedAt()).isEqualTo(now);
        assertThat(reg.getUpdatedBy()).isEqualTo(cancelActor);
    }

    @Test
    void cancelByStudent_emitsRegistrationCancelledEvent() {
        AttendanceRegistration reg = AttendanceRegistration.register(
                SESSION_ID, TENANT_ID, CLASS_ID, STUDENT_ID, ENROLLMENT_ID, MEMBERSHIP_ID,
                LEVEL, 1, 60, DATE, START, END, ACTOR_ID
        );
        reg.clearDomainEvents();

        reg.cancelByStudent(ACTOR_ID, Instant.now());

        assertThat(reg.getDomainEvents()).hasSize(1);
        assertThat(reg.getDomainEvents().get(0)).isInstanceOf(RegistrationCancelled.class);
        RegistrationCancelled event = (RegistrationCancelled) reg.getDomainEvents().get(0);
        assertThat(event.studentId()).isEqualTo(STUDENT_ID);
        assertThat(event.classId()).isEqualTo(CLASS_ID);
        assertThat(event.sessionDate()).isEqualTo(DATE);
    }

    @Test
    void cancelByStudent_onAlreadyCancelled_throwsIllegalState() {
        AttendanceRegistration reg = AttendanceRegistration.register(
                SESSION_ID, TENANT_ID, CLASS_ID, STUDENT_ID, ENROLLMENT_ID, MEMBERSHIP_ID,
                LEVEL, 1, 60, DATE, START, END, ACTOR_ID
        );
        reg.cancelByStudent(ACTOR_ID, Instant.now());

        assertThatThrownBy(() -> reg.cancelByStudent(ACTOR_ID, Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELLED_BY_STUDENT");
    }

    @Test
    void cancelByStudent_nullActorId_throwsNullPointer() {
        AttendanceRegistration reg = AttendanceRegistration.register(
                SESSION_ID, TENANT_ID, CLASS_ID, STUDENT_ID, ENROLLMENT_ID, MEMBERSHIP_ID,
                LEVEL, 1, 60, DATE, START, END, ACTOR_ID
        );

        assertThatThrownBy(() -> reg.cancelByStudent(null, Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }

    // ---------------------------------------------------------------
    // markPresent()
    // ---------------------------------------------------------------

    @Test
    void markPresent_onRegistered_transitionsToPresent() {
        AttendanceRegistration reg = buildRegistered();
        Instant now = Instant.now();
        UUID marker = UUID.randomUUID();

        reg.clearDomainEvents();
        reg.markPresent(marker, now);

        assertThat(reg.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
        assertThat(reg.getMarkedAt()).isEqualTo(now);
        assertThat(reg.getMarkedBy()).isEqualTo(marker);
        assertThat(reg.getUpdatedAt()).isEqualTo(now);
        assertThat(reg.getUpdatedBy()).isEqualTo(marker);
    }

    @Test
    void markPresent_emitsAttendanceMarkedPresentEvent() {
        AttendanceRegistration reg = buildRegistered();
        reg.clearDomainEvents();
        reg.markPresent(ACTOR_ID, Instant.now());

        assertThat(reg.getDomainEvents()).hasSize(1);
        assertThat(reg.getDomainEvents().get(0)).isInstanceOf(AttendanceMarkedPresent.class);
        AttendanceMarkedPresent ev = (AttendanceMarkedPresent) reg.getDomainEvents().get(0);
        assertThat(ev.studentId()).isEqualTo(STUDENT_ID);
        assertThat(ev.classId()).isEqualTo(CLASS_ID);
        assertThat(ev.membershipId()).isEqualTo(MEMBERSHIP_ID);
    }

    @Test
    void markPresent_notRegistered_throws() {
        AttendanceRegistration reg = buildRegistered();
        reg.cancelByStudent(ACTOR_ID, Instant.now());
        assertThatThrownBy(() -> reg.markPresent(ACTOR_ID, Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------------------------------------------------------------
    // markPresentNoHours()
    // ---------------------------------------------------------------

    @Test
    void markPresentNoHours_onRegistered_transitionsToPresentNoHours() {
        AttendanceRegistration reg = buildRegistered();
        reg.clearDomainEvents();
        Instant now = Instant.now();
        reg.markPresentNoHours(ACTOR_ID, now);

        assertThat(reg.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT_NO_HOURS);
        assertThat(reg.getMarkedAt()).isEqualTo(now);
    }

    @Test
    void markPresentNoHours_emitsAttendanceMarkedPresentNoHoursEvent() {
        AttendanceRegistration reg = buildRegistered();
        reg.clearDomainEvents();
        reg.markPresentNoHours(ACTOR_ID, Instant.now());

        assertThat(reg.getDomainEvents()).hasSize(1);
        assertThat(reg.getDomainEvents().get(0)).isInstanceOf(AttendanceMarkedPresentNoHours.class);
    }

    // ---------------------------------------------------------------
    // markAbsent()
    // ---------------------------------------------------------------

    @Test
    void markAbsent_onRegistered_transitionsToAbsent() {
        AttendanceRegistration reg = buildRegistered();
        reg.clearDomainEvents();
        Instant now = Instant.now();
        reg.markAbsent(ACTOR_ID, now);

        assertThat(reg.getStatus()).isEqualTo(AttendanceRegistrationStatus.ABSENT);
        assertThat(reg.getMarkedAt()).isEqualTo(now);
    }

    @Test
    void markAbsent_emitsAttendanceMarkedAbsentEvent() {
        AttendanceRegistration reg = buildRegistered();
        reg.clearDomainEvents();
        reg.markAbsent(ACTOR_ID, Instant.now());

        assertThat(reg.getDomainEvents()).hasSize(1);
        assertThat(reg.getDomainEvents().get(0)).isInstanceOf(AttendanceMarkedAbsent.class);
    }

    // ---------------------------------------------------------------
    // correctToAbsent()
    // ---------------------------------------------------------------

    @Test
    void correctToAbsent_fromPresent_transitionsAndSetsCorrectionFields() {
        AttendanceRegistration reg = buildRegistered();
        reg.markPresent(ACTOR_ID, Instant.now());
        reg.clearDomainEvents();

        UUID corrector = UUID.randomUUID();
        Instant correctedAt = Instant.now();
        reg.correctToAbsent(corrector, correctedAt, "Error in marking");

        assertThat(reg.getStatus()).isEqualTo(AttendanceRegistrationStatus.ABSENT);
        assertThat(reg.getCorrectedAt()).isEqualTo(correctedAt);
        assertThat(reg.getCorrectedBy()).isEqualTo(corrector);
        assertThat(reg.getCorrectionReason()).isEqualTo("Error in marking");
    }

    @Test
    void correctToAbsent_fromPresentNoHours_transitions() {
        AttendanceRegistration reg = buildRegistered();
        reg.markPresentNoHours(ACTOR_ID, Instant.now());
        reg.clearDomainEvents();

        reg.correctToAbsent(ACTOR_ID, Instant.now(), "Wrong student");

        assertThat(reg.getStatus()).isEqualTo(AttendanceRegistrationStatus.ABSENT);
    }

    @Test
    void correctToAbsent_emitsAttendanceCorrectedEvent() {
        AttendanceRegistration reg = buildRegistered();
        reg.markPresent(ACTOR_ID, Instant.now());
        reg.clearDomainEvents();

        reg.correctToAbsent(ACTOR_ID, Instant.now(), "reason");

        assertThat(reg.getDomainEvents()).hasSize(1);
        assertThat(reg.getDomainEvents().get(0)).isInstanceOf(AttendanceCorrected.class);
        AttendanceCorrected ev = (AttendanceCorrected) reg.getDomainEvents().get(0);
        assertThat(ev.previousStatus()).isEqualTo("PRESENT");
        assertThat(ev.newStatus()).isEqualTo("ABSENT");
    }

    @Test
    void correctToAbsent_fromRegistered_throws() {
        AttendanceRegistration reg = buildRegistered();
        assertThatThrownBy(() -> reg.correctToAbsent(ACTOR_ID, Instant.now(), "reason"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------------------------------------------------------------
    // correctToPresent()
    // ---------------------------------------------------------------

    @Test
    void correctToPresent_fromAbsent_transitionsToPresent() {
        AttendanceRegistration reg = buildRegistered();
        reg.markAbsent(ACTOR_ID, Instant.now());
        reg.clearDomainEvents();

        reg.correctToPresent(ACTOR_ID, Instant.now(), "Was actually present");

        assertThat(reg.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
    }

    @Test
    void correctToPresent_emitsAttendanceCorrectedEvent() {
        AttendanceRegistration reg = buildRegistered();
        reg.markAbsent(ACTOR_ID, Instant.now());
        reg.clearDomainEvents();

        reg.correctToPresent(ACTOR_ID, Instant.now(), "reason");

        AttendanceCorrected ev = (AttendanceCorrected) reg.getDomainEvents().get(0);
        assertThat(ev.previousStatus()).isEqualTo("ABSENT");
        assertThat(ev.newStatus()).isEqualTo("PRESENT");
    }

    @Test
    void correctToPresent_notFromAbsent_throws() {
        AttendanceRegistration reg = buildRegistered();
        assertThatThrownBy(() -> reg.correctToPresent(ACTOR_ID, Instant.now(), "reason"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------------------------------------------------------------
    // correctToPresentNoHours()
    // ---------------------------------------------------------------

    @Test
    void correctToPresentNoHours_fromAbsent_transitions() {
        AttendanceRegistration reg = buildRegistered();
        reg.markAbsent(ACTOR_ID, Instant.now());
        reg.clearDomainEvents();

        reg.correctToPresentNoHours(ACTOR_ID, Instant.now(), "No hours left");

        assertThat(reg.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT_NO_HOURS);
    }

    // ---------------------------------------------------------------
    // markPresentByStaff()
    // ---------------------------------------------------------------

    @Test
    void markPresentByStaff_overridesIntendedHours_andTransitionsToPresent() {
        AttendanceRegistration reg = sampleRegistered(/*intendedHours=*/2, /*durationMinutes=*/120);
        Instant now = Instant.parse("2026-04-27T17:00:00Z");
        UUID actor = UUID.randomUUID();

        reg.markPresentByStaff(actor, now, /*hoursToCharge=*/1, /*classDurationMinutes=*/120);

        assertThat(reg.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
        assertThat(reg.getIntendedHours()).isEqualTo(1);
        assertThat(reg.getMarkedAt()).isEqualTo(now);
        assertThat(reg.getMarkedBy()).isEqualTo(actor);
    }

    @Test
    void markPresentByStaff_emitsMarkedPresentEvent_withOverriddenHours() {
        AttendanceRegistration reg = sampleRegistered(2, 120);
        UUID actor = UUID.randomUUID();
        Instant now = Instant.now();

        reg.markPresentByStaff(actor, now, 1, 120);

        AttendanceMarkedPresent ev = (AttendanceMarkedPresent) reg.getDomainEvents().stream()
                .filter(e -> e instanceof AttendanceMarkedPresent)
                .findFirst().orElseThrow();
        assertThat(ev.intendedHours()).isEqualTo(1);
        assertThat(ev.actorId()).isEqualTo(actor);
    }

    @Test
    void markPresentByStaff_rejectsWhenNotRegistered() {
        AttendanceRegistration reg = sampleRegistered(1, 60);
        // transition to PRESENT first via normal path
        reg.markPresent(UUID.randomUUID(), Instant.now());

        assertThatThrownBy(() -> reg.markPresentByStaff(UUID.randomUUID(), Instant.now(), 1, 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot mark present");
    }

    @Test
    void markPresentByStaff_rejectsHoursOutOfRange_zero() {
        AttendanceRegistration reg = sampleRegistered(1, 60);
        assertThatThrownBy(() -> reg.markPresentByStaff(UUID.randomUUID(), Instant.now(), 0, 60))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void markPresentByStaff_rejectsHoursOutOfRange_aboveDurationFloor() {
        AttendanceRegistration reg = sampleRegistered(1, 90);
        // floor(90/60) = 1, so 2 must be rejected
        assertThatThrownBy(() -> reg.markPresentByStaff(UUID.randomUUID(), Instant.now(), 2, 90))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hoursToCharge");
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------

    private static AttendanceRegistration buildRegistered() {
        return AttendanceRegistration.register(
                SESSION_ID, TENANT_ID, CLASS_ID, STUDENT_ID, ENROLLMENT_ID, MEMBERSHIP_ID,
                LEVEL, 1, 60, DATE, START, END, ACTOR_ID
        );
    }

    private static AttendanceRegistration sampleRegistered(int intendedHours, int durationMinutes) {
        LocalTime end = START.plusMinutes(durationMinutes);
        return AttendanceRegistration.register(
                SESSION_ID, TENANT_ID, CLASS_ID, STUDENT_ID, ENROLLMENT_ID, MEMBERSHIP_ID,
                LEVEL, intendedHours, durationMinutes, DATE, START, end, ACTOR_ID
        );
    }
}
