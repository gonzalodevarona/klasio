package com.klasio.attendance.application.util;

import com.klasio.attendance.application.util.ClassScheduleExpander.SessionTuple;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClassScheduleExpanderLocationTest {

    @Test
    void recurringTupleCarriesEntryLocation() {
        UUID classId = UUID.randomUUID();
        ClassRegistrationView cls = new ClassRegistrationView(
                classId, UUID.randomUUID(), UUID.randomUUID(),
                "BEGINNER", "ACTIVE", "RECURRING", 20, "Yoga",
                List.of(new ScheduleEntryView(
                        DayOfWeek.MONDAY, null,
                        LocalTime.of(9, 0), LocalTime.of(10, 0),
                        "Salon 1")));

        LocalDate monday = LocalDate.of(2026, 6, 1); // Monday

        List<SessionTuple> tuples = ClassScheduleExpander.expand(List.of(cls), monday, monday);

        assertThat(tuples).hasSize(1);
        assertThat(tuples.get(0).location()).isEqualTo("Salon 1");
    }

    @Test
    void oneTimeTupleCarriesEntryLocation() {
        UUID classId = UUID.randomUUID();
        LocalDate specificDate = LocalDate.of(2026, 6, 3);
        ClassRegistrationView cls = new ClassRegistrationView(
                classId, UUID.randomUUID(), UUID.randomUUID(),
                "BEGINNER", "ACTIVE", "ONE_TIME", 20, "Yoga",
                List.of(new ScheduleEntryView(
                        null, specificDate,
                        LocalTime.of(9, 0), LocalTime.of(10, 0),
                        "Court B")));

        List<SessionTuple> tuples = ClassScheduleExpander.expand(List.of(cls), specificDate, specificDate);

        assertThat(tuples).hasSize(1);
        assertThat(tuples.get(0).location()).isEqualTo("Court B");
    }

    @Test
    void nullLocationIsPreservedInTuple() {
        UUID classId = UUID.randomUUID();
        LocalDate monday = LocalDate.of(2026, 6, 1);
        ClassRegistrationView cls = new ClassRegistrationView(
                classId, UUID.randomUUID(), UUID.randomUUID(),
                "BEGINNER", "ACTIVE", "RECURRING", 20, "Yoga",
                List.of(new ScheduleEntryView(
                        DayOfWeek.MONDAY, null,
                        LocalTime.of(9, 0), LocalTime.of(10, 0),
                        null)));

        List<SessionTuple> tuples = ClassScheduleExpander.expand(List.of(cls), monday, monday);

        assertThat(tuples).hasSize(1);
        assertThat(tuples.get(0).location()).isNull();
    }
}
