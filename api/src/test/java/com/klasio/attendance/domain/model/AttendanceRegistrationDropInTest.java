package com.klasio.attendance.domain.model;

import com.klasio.attendance.domain.event.DropInAttendanceMarked;
import com.klasio.shared.domain.DomainEvent;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class AttendanceRegistrationDropInTest {

    private final UUID tenant   = UUID.randomUUID();
    private final UUID session  = UUID.randomUUID();
    private final UUID classId  = UUID.randomUUID();
    private final UUID attendee = UUID.randomUUID();
    private final UUID payment  = UUID.randomUUID();
    private final UUID actor    = UUID.randomUUID();
    private final Instant now   = Instant.now();

    @Test
    void createDropIn_setsStatusPresent() {
        var reg = AttendanceRegistration.createDropIn(
            tenant, session, classId, LocalDate.now(), LocalTime.of(6,0), LocalTime.of(7,0),
            attendee, payment, actor, now);
        assertThat(reg.getStatus()).isEqualTo(AttendanceRegistrationStatus.PRESENT);
    }

    @Test
    void createDropIn_studentFieldsAreNull() {
        var reg = AttendanceRegistration.createDropIn(
            tenant, session, classId, LocalDate.now(), LocalTime.of(6,0), LocalTime.of(7,0),
            attendee, payment, actor, now);
        assertThat(reg.getStudentId()).isNull();
        assertThat(reg.getEnrollmentId()).isNull();
        assertThat(reg.getMembershipId()).isNull();
        assertThat(reg.getLevelAtRegistration()).isNull();
        assertThat(reg.getIntendedHours()).isNull();
    }

    @Test
    void createDropIn_dropInFieldsAreSet() {
        var reg = AttendanceRegistration.createDropIn(
            tenant, session, classId, LocalDate.now(), LocalTime.of(6,0), LocalTime.of(7,0),
            attendee, payment, actor, now);
        assertThat(reg.getDropInAttendeeId()).isEqualTo(attendee);
        assertThat(reg.getDropInPaymentId()).isEqualTo(payment);
    }

    @Test
    void createDropIn_emitsSingleEvent() {
        var reg = AttendanceRegistration.createDropIn(
            tenant, session, classId, LocalDate.now(), LocalTime.of(6,0), LocalTime.of(7,0),
            attendee, payment, actor, now);
        assertThat(reg.getDomainEvents()).hasSize(1);
        assertThat(reg.getDomainEvents().get(0)).isInstanceOf(DropInAttendanceMarked.class);
    }

    @Test
    void createDropIn_rejectsNullAttendeeId() {
        assertThatThrownBy(() ->
            AttendanceRegistration.createDropIn(
                tenant, session, classId, LocalDate.now(), LocalTime.of(6,0), LocalTime.of(7,0),
                null, payment, actor, now))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createDropIn_rejectsNullPaymentId() {
        assertThatThrownBy(() ->
            AttendanceRegistration.createDropIn(
                tenant, session, classId, LocalDate.now(), LocalTime.of(6,0), LocalTime.of(7,0),
                attendee, null, actor, now))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
