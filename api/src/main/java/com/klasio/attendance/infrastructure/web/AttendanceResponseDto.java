package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.AttendanceRegistrationView;
import com.klasio.attendance.application.dto.AvailableSessionView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class AttendanceResponseDto {

    public record RegistrationResponse(
            UUID id,
            UUID sessionId,
            UUID classId,
            String className,
            LocalDate sessionDate,
            LocalTime sessionStartTime,
            LocalTime sessionEndTime,
            String level,
            int intendedHours,
            String status,
            Instant createdAt
    ) {
        public static RegistrationResponse from(AttendanceRegistrationView view) {
            return new RegistrationResponse(
                    view.id(),
                    view.sessionId(),
                    view.classId(),
                    view.className(),
                    view.sessionDate(),
                    view.sessionStartTime(),
                    view.sessionEndTime(),
                    view.level(),
                    view.intendedHours(),
                    view.status(),
                    view.createdAt()
            );
        }
    }

    public record AvailableSessionResponse(
            UUID classId,
            String className,
            UUID sessionId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String level,
            UUID programId,
            int currentCapacity,
            int maxStudents,
            String status
    ) {
        public static AvailableSessionResponse from(AvailableSessionView view) {
            return new AvailableSessionResponse(
                    view.classId(),
                    view.className(),
                    view.sessionId(),
                    view.sessionDate(),
                    view.startTime(),
                    view.endTime(),
                    view.level(),
                    view.programId(),
                    view.currentCapacity(),
                    view.maxStudents(),
                    view.status()
            );
        }
    }
}
