package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.AttendanceRegistrationView;
import com.klasio.attendance.application.dto.AttendanceStatsView;
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
            Instant createdAt,
            String sessionCancellationReason,
            String sessionStatus,
            String sessionAlertReason
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
                    view.createdAt(),
                    view.sessionCancellationReason(),
                    view.sessionStatus(),
                    view.sessionAlertReason()
            );
        }
    }

    public record AttendanceStatsResponse(
            long attended,
            long cancelledByStudent,
            long cancelledBySystem,
            long absent,
            long totalHoursConsumed,
            int attendanceRatePercent
    ) {
        public static AttendanceStatsResponse from(AttendanceStatsView view) {
            return new AttendanceStatsResponse(
                    view.attended(),
                    view.cancelledByStudent(),
                    view.cancelledBySystem(),
                    view.absent(),
                    view.totalHoursConsumed(),
                    view.attendanceRatePercent()
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
            String status,
            boolean registrationOpen,
            String alertReason,
            UUID registrationId,
            String registrationStatus
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
                    view.status(),
                    view.registrationOpen(),
                    view.alertReason(),
                    view.registrationId(),
                    view.registrationStatus()
            );
        }
    }
}
