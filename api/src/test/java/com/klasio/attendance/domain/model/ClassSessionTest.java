package com.klasio.attendance.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ClassSessionTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2026, 5, 4);
    private static final LocalTime START = LocalTime.of(18, 0);
    private static final LocalTime END = LocalTime.of(19, 0);

    @Test
    void materialize_validArgs_createsScheduledSession() {
        ClassSession session = ClassSession.materialize(TENANT_ID, CLASS_ID, DATE, START, END, ACTOR_ID);

        assertThat(session.getId()).isNotNull();
        assertThat(session.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(session.getClassId()).isEqualTo(CLASS_ID);
        assertThat(session.getSessionDate()).isEqualTo(DATE);
        assertThat(session.getStartTime()).isEqualTo(START);
        assertThat(session.getEndTime()).isEqualTo(END);
        assertThat(session.getCurrentCapacity()).isZero();
        assertThat(session.getStatus()).isEqualTo(ClassSessionStatus.SCHEDULED);
        assertThat(session.getCreatedAt()).isNotNull();
        assertThat(session.getCreatedBy()).isEqualTo(ACTOR_ID);
    }

    @Test
    void materialize_endTimeBeforeStart_throws() {
        assertThatThrownBy(() ->
                ClassSession.materialize(TENANT_ID, CLASS_ID, DATE, END, START, ACTOR_ID)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime must be after startTime");
    }

    @Test
    void materialize_nullArgs_throws() {
        assertThatThrownBy(() ->
                ClassSession.materialize(null, CLASS_ID, DATE, START, END, ACTOR_ID)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void cancel_scheduledSession_transitionsToCancelled() {
        ClassSession session = ClassSession.materialize(TENANT_ID, CLASS_ID, DATE, START, END, ACTOR_ID);
        session.cancel("Professor is sick", ACTOR_ID);

        assertThat(session.getStatus()).isEqualTo(ClassSessionStatus.CANCELLED);
        assertThat(session.getCancellationReason()).isEqualTo("Professor is sick");
        assertThat(session.getCancelledBy()).isEqualTo(ACTOR_ID);
        assertThat(session.getCancelledAt()).isNotNull();
    }

    @Test
    void cancel_alreadyCancelled_throws() {
        ClassSession session = ClassSession.materialize(TENANT_ID, CLASS_ID, DATE, START, END, ACTOR_ID);
        session.cancel("Reason", ACTOR_ID);

        assertThatThrownBy(() -> session.cancel("Another reason", ACTOR_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void raiseAlert_scheduledSession_transitionsToAlerted() {
        ClassSession session = ClassSession.materialize(TENANT_ID, CLASS_ID, DATE, START, END, ACTOR_ID);
        session.raiseAlert("Low attendance expected", ACTOR_ID);

        assertThat(session.getStatus()).isEqualTo(ClassSessionStatus.ALERTED);
        assertThat(session.getAlertReason()).isEqualTo("Low attendance expected");
    }

    @Test
    void raiseAlert_cancelledSession_throws() {
        ClassSession session = ClassSession.materialize(TENANT_ID, CLASS_ID, DATE, START, END, ACTOR_ID);
        session.cancel("Reason", ACTOR_ID);

        assertThatThrownBy(() -> session.raiseAlert("Alert", ACTOR_ID))
                .isInstanceOf(IllegalStateException.class);
    }
}
