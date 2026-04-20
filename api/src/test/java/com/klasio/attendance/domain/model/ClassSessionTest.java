package com.klasio.attendance.domain.model;

import com.klasio.attendance.domain.event.SessionAlertRaised;
import com.klasio.attendance.domain.event.SessionAlertUpdated;
import com.klasio.attendance.domain.event.SessionCancelled;
import com.klasio.attendance.domain.exception.AlertAuthorViolationException;
import com.klasio.shared.domain.DomainEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ClassSessionTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CLASS_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2026, 5, 4);
    private static final LocalTime START = LocalTime.of(18, 0);
    private static final LocalTime END = LocalTime.of(19, 0);

    private static final String REASON = "rain cancelled the outdoor court"; // 35 chars

    private ClassSession sample() {
        return ClassSession.materialize(
                UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0),
                UUID.randomUUID());
    }

    // ---------------------------------------------------------------
    // materialize() — unchanged baseline tests
    // ---------------------------------------------------------------

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

    // ---------------------------------------------------------------
    // raiseAlert() — new 3-arg signature + domain events + validation
    // ---------------------------------------------------------------

    @Test
    void raiseAlertEmitsSessionAlertRaisedEvent() {
        ClassSession s = sample();
        s.raiseAlert(REASON, UUID.randomUUID(), "PROFESSOR");
        assertThat(s.getStatus()).isEqualTo(ClassSessionStatus.ALERTED);
        assertThat(s.getAlertReason()).isEqualTo(REASON);
        List<DomainEvent> events = s.getDomainEvents();
        assertThat(events).hasSize(1).first().isInstanceOf(SessionAlertRaised.class);
        SessionAlertRaised event = (SessionAlertRaised) events.get(0);
        assertThat(event.reason()).isEqualTo(REASON);
        assertThat(event.actorRole()).isEqualTo("PROFESSOR");
        assertThat(event.sessionDate()).isNotNull();
        assertThat(event.startTime()).isNotNull();
        assertThat(event.endTime()).isNotNull();
    }

    @Test
    void raiseAlertRejectsReasonBelow20Chars() {
        ClassSession s = sample();
        assertThatThrownBy(() -> s.raiseAlert("too short", UUID.randomUUID(), "PROFESSOR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20");
    }

    @Test
    void raiseAlertRejectsCancelledSession() {
        ClassSession s = sample();
        s.cancel(REASON, UUID.randomUUID(), "ADMIN");
        s.clearDomainEvents();
        assertThatThrownBy(() -> s.raiseAlert(REASON, UUID.randomUUID(), "PROFESSOR"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---------------------------------------------------------------
    // updateAlertReason()
    // ---------------------------------------------------------------

    @Test
    void updateAlertReasonRequiresALERTEDStatus() {
        ClassSession s = sample();
        assertThatThrownBy(() -> s.updateAlertReason(REASON, UUID.randomUUID(), "PROFESSOR"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateAlertReasonRequiresOriginalAuthor() {
        ClassSession s = sample();
        UUID author = UUID.randomUUID();
        s.raiseAlert(REASON, author, "PROFESSOR");
        s.clearDomainEvents();
        assertThatThrownBy(() -> s.updateAlertReason("a different reason for the class", UUID.randomUUID(), "PROFESSOR"))
                .isInstanceOf(AlertAuthorViolationException.class);
    }

    @Test
    void updateAlertReasonEmitsSessionAlertUpdatedEvent() {
        ClassSession s = sample();
        UUID author = UUID.randomUUID();
        s.raiseAlert(REASON, author, "PROFESSOR");
        s.clearDomainEvents();
        String updated = "rain keeps going, now it is pouring hard";
        s.updateAlertReason(updated, author, "PROFESSOR");
        assertThat(s.getAlertReason()).isEqualTo(updated);
        assertThat(s.getDomainEvents()).hasSize(1).first().isInstanceOf(SessionAlertUpdated.class);
    }

    // ---------------------------------------------------------------
    // cancel() — new 3-arg signature + domain events + validation
    // ---------------------------------------------------------------

    @Test
    void cancelEmitsSessionCancelledEvent() {
        ClassSession s = sample();
        s.cancel(REASON, UUID.randomUUID(), "ADMIN");
        assertThat(s.getStatus()).isEqualTo(ClassSessionStatus.CANCELLED);
        assertThat(s.getDomainEvents()).hasSize(1).first().isInstanceOf(SessionCancelled.class);
        SessionCancelled cancelEvent = (SessionCancelled) s.getDomainEvents().get(0);
        assertThat(cancelEvent.reason()).isEqualTo(REASON);
        assertThat(cancelEvent.actorRole()).isEqualTo("ADMIN");
        assertThat(cancelEvent.affectedStudentIds()).isEmpty();
        assertThat(cancelEvent.sessionDate()).isNotNull();
        assertThat(cancelEvent.startTime()).isNotNull();
        assertThat(cancelEvent.endTime()).isNotNull();
    }

    @Test
    void cancelRejectsReasonBelow20Chars() {
        ClassSession s = sample();
        assertThatThrownBy(() -> s.cancel("short", UUID.randomUUID(), "ADMIN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelIsTerminal() {
        ClassSession s = sample();
        s.cancel(REASON, UUID.randomUUID(), "ADMIN");
        assertThatThrownBy(() -> s.cancel(REASON, UUID.randomUUID(), "ADMIN"))
                .isInstanceOf(IllegalStateException.class);
    }
}
