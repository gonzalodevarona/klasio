package com.klasio.program.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScheduleEntryTest {

    @Test
    @DisplayName("should create valid schedule entry")
    void create_withValidInputs_succeeds() {
        ScheduleEntry entry = new ScheduleEntry(
                DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(20, 0));

        assertEquals(DayOfWeek.MONDAY, entry.dayOfWeek());
        assertEquals(LocalTime.of(18, 0), entry.startTime());
        assertEquals(LocalTime.of(20, 0), entry.endTime());
    }

    @Test
    @DisplayName("should reject null dayOfWeek")
    void create_withNullDay_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new ScheduleEntry(null, LocalTime.of(18, 0), LocalTime.of(20, 0)));
    }

    @Test
    @DisplayName("should reject null startTime")
    void create_withNullStartTime_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new ScheduleEntry(DayOfWeek.MONDAY, null, LocalTime.of(20, 0)));
    }

    @Test
    @DisplayName("should reject null endTime")
    void create_withNullEndTime_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new ScheduleEntry(DayOfWeek.MONDAY, LocalTime.of(18, 0), null));
    }

    @Test
    @DisplayName("should reject endTime before startTime")
    void create_withEndBeforeStart_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScheduleEntry(DayOfWeek.MONDAY, LocalTime.of(20, 0), LocalTime.of(18, 0)));
    }

    @Test
    @DisplayName("should reject endTime equal to startTime")
    void create_withEqualTimes_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScheduleEntry(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(18, 0)));
    }
}
