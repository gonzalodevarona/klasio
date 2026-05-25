package com.klasio.programclass.domain.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassScheduleEntryLocationTest {

    private ClassScheduleEntry entryWithLocation(String location) {
        return new ClassScheduleEntry(
                DayOfWeek.MONDAY, null,
                LocalTime.of(10, 0), LocalTime.of(11, 0),
                location);
    }

    @Test
    void titleCasesEachWord() {
        assertThat(entryWithLocation("cancha norte").location()).isEqualTo("Cancha Norte");
    }

    @Test
    void titleCasesSingleWordWithNumber() {
        assertThat(entryWithLocation("salon 1").location()).isEqualTo("Salon 1");
    }

    @Test
    void trimsAndCollapsesInternalWhitespace() {
        assertThat(entryWithLocation("  coliseo   2 ").location()).isEqualTo("Coliseo 2");
    }

    @Test
    void blankBecomesNull() {
        assertThat(entryWithLocation("   ").location()).isNull();
    }

    @Test
    void nullStaysNull() {
        assertThat(entryWithLocation(null).location()).isNull();
    }

    @Test
    void rejectsLocationLongerThan60Chars() {
        String tooLong = "a".repeat(61);
        assertThatThrownBy(() -> entryWithLocation(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("60");
    }
}
