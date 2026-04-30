package com.klasio.attendance.application.util;

import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Expands class schedule entries into concrete session tuples within a date window.
 * Shared by GetAvailableSessionsService and ListClassSessionRosterService to guarantee
 * both services apply identical recurrence rules.
 */
public final class ClassScheduleExpander {

    private ClassScheduleExpander() {}

    public static List<SessionTuple> expand(List<ClassRegistrationView> classes,
                                             LocalDate from,
                                             LocalDate to) {
        List<SessionTuple> tuples = new ArrayList<>();
        for (ClassRegistrationView cls : classes) {
            for (ScheduleEntryView entry : cls.scheduleEntries()) {
                if ("ONE_TIME".equals(cls.type())) {
                    LocalDate specificDate = entry.specificDate();
                    if (specificDate != null
                            && !specificDate.isBefore(from)
                            && !specificDate.isAfter(to)) {
                        tuples.add(new SessionTuple(cls.id(), specificDate, entry.startTime(), entry.endTime()));
                    }
                } else {
                    LocalDate cursor = from;
                    while (!cursor.isAfter(to)) {
                        if (entry.dayOfWeek() != null && cursor.getDayOfWeek().equals(entry.dayOfWeek())) {
                            tuples.add(new SessionTuple(cls.id(), cursor, entry.startTime(), entry.endTime()));
                        }
                        cursor = cursor.plusDays(1);
                    }
                }
            }
        }
        return tuples;
    }

    public record SessionTuple(
            UUID classId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime
    ) {}
}
