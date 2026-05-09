package com.klasio.dropin.domain.model;

import com.klasio.dropin.domain.event.DropInAttendeeRegistered;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class DropInAttendeeTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final String FULL_NAME = "Jane Doe";
    private static final String PHONE = "+573001234567";

    private DropInAttendee createDefault() {
        return DropInAttendee.create(TENANT_ID, FULL_NAME, PHONE, ACTOR_ID, Instant.now());
    }

    @Test
    void create_rejectsBlankFullName() {
        assertThatThrownBy(() -> DropInAttendee.create(TENANT_ID, "  ", PHONE, ACTOR_ID, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_rejectsBlankPhone() {
        assertThatThrownBy(() -> DropInAttendee.create(TENANT_ID, FULL_NAME, "", ACTOR_ID, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_initialCountersAreZero() {
        DropInAttendee attendee = createDefault();

        assertThat(attendee.getTotalVisits()).isZero();
        assertThat(attendee.getFirstVisitAt()).isNull();
        assertThat(attendee.getLastVisitAt()).isNull();
    }

    @Test
    void create_emitsDomainEvent() {
        DropInAttendee attendee = createDefault();

        assertThat(attendee.getDomainEvents()).hasSize(1);
        assertThat(attendee.getDomainEvents().get(0)).isInstanceOf(DropInAttendeeRegistered.class);

        DropInAttendeeRegistered event = (DropInAttendeeRegistered) attendee.getDomainEvents().get(0);
        assertThat(event.fullName()).isEqualTo(FULL_NAME);
        assertThat(event.phone()).isEqualTo(PHONE);
        assertThat(event.tenantId()).isEqualTo(TENANT_ID);
        assertThat(event.actorId()).isEqualTo(ACTOR_ID);
    }

    @Test
    void recordVisit_incrementsCounterAndSetsTimestamps() {
        DropInAttendee attendee = createDefault();
        Instant visitTime = Instant.now();

        attendee.recordVisit(visitTime);

        assertThat(attendee.getTotalVisits()).isEqualTo(1);
        assertThat(attendee.getFirstVisitAt()).isEqualTo(visitTime);
        assertThat(attendee.getLastVisitAt()).isEqualTo(visitTime);
    }

    @Test
    void recordVisit_firstVisitAtIsSticky() {
        DropInAttendee attendee = createDefault();
        Instant first = Instant.now();
        Instant second = first.plusSeconds(3600);

        attendee.recordVisit(first);
        attendee.recordVisit(second);

        assertThat(attendee.getTotalVisits()).isEqualTo(2);
        assertThat(attendee.getFirstVisitAt()).isEqualTo(first);
        assertThat(attendee.getLastVisitAt()).isEqualTo(second);
    }

    @Test
    void convertToStudent_setsFields() {
        DropInAttendee attendee = createDefault();
        UUID studentId = UUID.randomUUID();
        Instant convertedAt = Instant.now();

        attendee.convertToStudent(studentId, convertedAt);

        assertThat(attendee.getConvertedToStudentId()).isEqualTo(studentId);
        assertThat(attendee.getConvertedAt()).isEqualTo(convertedAt);
    }

    @Test
    void convertToStudent_rejectsDoubleConversion() {
        DropInAttendee attendee = createDefault();
        UUID studentId = UUID.randomUUID();
        Instant now = Instant.now();

        attendee.convertToStudent(studentId, now);

        assertThatThrownBy(() -> attendee.convertToStudent(UUID.randomUUID(), Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }
}
