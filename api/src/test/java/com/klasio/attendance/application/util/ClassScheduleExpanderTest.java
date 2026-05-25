package com.klasio.attendance.application.util;

import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClassScheduleExpanderTest {

    private static final UUID CLASS_ID  = UUID.randomUUID();
    private static final UUID CLASS_ID2 = UUID.randomUUID();
    private static final LocalTime START = LocalTime.of(18, 0);
    private static final LocalTime END   = LocalTime.of(19, 0);

    // Monday 2026-04-27
    private static final LocalDate MONDAY    = LocalDate.of(2026, 4, 27);
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 4, 29);
    private static final LocalDate FRIDAY    = LocalDate.of(2026, 5,  1);
    private static final LocalDate SUNDAY    = LocalDate.of(2026, 5,  3);

    // ── Case 1: recurring MON, window Mon–Sun → 1 tuple ──────────────────────

    @Test
    void recurringMonday_windowMonToSun_returnsOneTupleOnMonday() {
        ClassRegistrationView cls = recurring(CLASS_ID, DayOfWeek.MONDAY, START, END);

        List<ClassScheduleExpander.SessionTuple> result =
                ClassScheduleExpander.expand(List.of(cls), MONDAY, SUNDAY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sessionDate()).isEqualTo(MONDAY);
        assertThat(result.get(0).classId()).isEqualTo(CLASS_ID);
    }

    // ── Case 2: recurring MON+WED, 2-week window → 4 tuples ─────────────────

    @Test
    void recurringMonWed_twoWeeks_returnsFourTuples() {
        ClassRegistrationView cls = recurringMulti(CLASS_ID,
                List.of(
                        scheduleEntry(DayOfWeek.MONDAY, START, END),
                        scheduleEntry(DayOfWeek.WEDNESDAY, START, END)
                ), "RECURRING");

        LocalDate from = MONDAY;              // Mon Apr 27
        LocalDate to   = MONDAY.plusDays(13); // Sun May 10

        List<ClassScheduleExpander.SessionTuple> result =
                ClassScheduleExpander.expand(List.of(cls), from, to);

        assertThat(result).hasSize(4);
        assertThat(result).extracting(ClassScheduleExpander.SessionTuple::sessionDate)
                .containsExactlyInAnyOrder(
                        LocalDate.of(2026, 4, 27), // Mon
                        LocalDate.of(2026, 4, 29), // Wed
                        LocalDate.of(2026, 5,  4), // Mon
                        LocalDate.of(2026, 5,  6)  // Wed
                );
    }

    // ── Case 3: ONE_TIME inside window → 1 tuple ─────────────────────────────

    @Test
    void oneTime_insideWindow_returnsOneTuple() {
        ClassRegistrationView cls = oneTime(CLASS_ID, WEDNESDAY, START, END);

        List<ClassScheduleExpander.SessionTuple> result =
                ClassScheduleExpander.expand(List.of(cls), MONDAY, SUNDAY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sessionDate()).isEqualTo(WEDNESDAY);
    }

    // ── Case 4: ONE_TIME outside window → 0 tuples ───────────────────────────

    @Test
    void oneTime_outsideWindow_returnsEmpty() {
        LocalDate outsideDate = SUNDAY.plusDays(1); // Mon May 4 — outside Mon-Sun
        ClassRegistrationView cls = oneTime(CLASS_ID, outsideDate, START, END);

        List<ClassScheduleExpander.SessionTuple> result =
                ClassScheduleExpander.expand(List.of(cls), MONDAY, SUNDAY);

        assertThat(result).isEmpty();
    }

    // ── Case 5: recurring MON, window starts Wed → first tuple next Mon ───────

    @Test
    void recurringMonday_windowStartsWed_firstTupleNextMonday() {
        ClassRegistrationView cls = recurring(CLASS_ID, DayOfWeek.MONDAY, START, END);

        LocalDate from = WEDNESDAY;                   // Apr 29 (Wed)
        LocalDate to   = WEDNESDAY.plusDays(6);       // May 5 (Tue)

        List<ClassScheduleExpander.SessionTuple> result =
                ClassScheduleExpander.expand(List.of(cls), from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sessionDate()).isEqualTo(LocalDate.of(2026, 5, 4)); // next Mon
    }

    // ── Case 6: from == to, weekday matches → 1 tuple ────────────────────────

    @Test
    void recurringMonday_fromEqualsTo_weekdayMatches_returnsOneTuple() {
        ClassRegistrationView cls = recurring(CLASS_ID, DayOfWeek.MONDAY, START, END);

        List<ClassScheduleExpander.SessionTuple> result =
                ClassScheduleExpander.expand(List.of(cls), MONDAY, MONDAY);

        assertThat(result).hasSize(1);
    }

    // ── Case 7: from == to, weekday does not match → 0 tuples ────────────────

    @Test
    void recurringMonday_fromEqualsTo_weekdayMismatch_returnsEmpty() {
        ClassRegistrationView cls = recurring(CLASS_ID, DayOfWeek.MONDAY, START, END);

        List<ClassScheduleExpander.SessionTuple> result =
                ClassScheduleExpander.expand(List.of(cls), WEDNESDAY, WEDNESDAY);

        assertThat(result).isEmpty();
    }

    // ── Case 8: empty class list → 0 tuples ──────────────────────────────────

    @Test
    void emptyClassList_returnsEmpty() {
        List<ClassScheduleExpander.SessionTuple> result =
                ClassScheduleExpander.expand(List.of(), MONDAY, SUNDAY);

        assertThat(result).isEmpty();
    }

    // ── Case 9: class with no schedule entries → 0 tuples ────────────────────

    @Test
    void classWithNoScheduleEntries_returnsEmpty() {
        ClassRegistrationView cls = recurringMulti(CLASS_ID, List.of(), "RECURRING");

        List<ClassScheduleExpander.SessionTuple> result =
                ClassScheduleExpander.expand(List.of(cls), MONDAY, SUNDAY);

        assertThat(result).isEmpty();
    }

    // ── Case 10: multiple classes, mixed types → union, no cross-contamination

    @Test
    void multipleClasses_mixedTypes_returnsCombinedUnion() {
        ClassRegistrationView recurring = recurring(CLASS_ID, DayOfWeek.MONDAY, START, END);
        ClassRegistrationView oneTime   = oneTime(CLASS_ID2, FRIDAY, START, END);

        List<ClassScheduleExpander.SessionTuple> result =
                ClassScheduleExpander.expand(List.of(recurring, oneTime), MONDAY, SUNDAY);

        // 1 Mon (recurring) + 1 Fri (one-time) = 2 tuples
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ClassScheduleExpander.SessionTuple::classId)
                .containsExactlyInAnyOrder(CLASS_ID, CLASS_ID2);
        assertThat(result).extracting(ClassScheduleExpander.SessionTuple::sessionDate)
                .containsExactlyInAnyOrder(MONDAY, FRIDAY);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ClassRegistrationView recurring(UUID classId, DayOfWeek day,
                                                    LocalTime start, LocalTime end) {
        return recurringMulti(classId, List.of(scheduleEntry(day, start, end)), "RECURRING");
    }

    private static ClassRegistrationView oneTime(UUID classId, LocalDate specificDate,
                                                  LocalTime start, LocalTime end) {
        ScheduleEntryView entry = new ScheduleEntryView(null, specificDate, start, end, null);
        return recurringMulti(classId, List.of(entry), "ONE_TIME");
    }

    private static ClassRegistrationView recurringMulti(UUID classId,
                                                         List<ScheduleEntryView> entries,
                                                         String type) {
        return new ClassRegistrationView(
                classId,
                UUID.randomUUID(), // programId
                null,              // professorId
                "BEGINNER",
                "ACTIVE",
                type,
                20,
                "Test Class",
                entries
        );
    }

    private static ScheduleEntryView scheduleEntry(DayOfWeek day, LocalTime start, LocalTime end) {
        return new ScheduleEntryView(day, null, start, end, null);
    }
}
