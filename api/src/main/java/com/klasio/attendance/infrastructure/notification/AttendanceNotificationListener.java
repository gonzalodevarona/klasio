package com.klasio.attendance.infrastructure.notification;

import com.klasio.attendance.domain.event.AttendanceCorrected;
import com.klasio.attendance.domain.event.AttendanceMarkedAbsent;
import com.klasio.attendance.domain.event.AttendanceMarkedPresent;
import com.klasio.attendance.domain.event.AttendanceMarkedPresentNoHours;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Fire-and-forget listener for attendance marking events that may require
 * external notifications (email, in-app). Runs asynchronously to avoid
 * slowing down the transactional flow.
 *
 * In v1.0 these log stubs will be replaced with real Postmark email calls (RF-32).
 */
@Slf4j
@Component
public class AttendanceNotificationListener {

    @Async
    @EventListener
    public void onAttendanceMarkedPresent(AttendanceMarkedPresent event) {
        log.info("[NOTIFY] Attendance marked PRESENT — studentId={}, classId={}, sessionDate={}, intendedHours={}",
                event.studentId(), event.classId(), event.sessionDate(), event.intendedHours());
        // TODO (RF-32): send attendance confirmation to student (Postmark)
    }

    @Async
    @EventListener
    public void onAttendanceMarkedAbsent(AttendanceMarkedAbsent event) {
        log.info("[NOTIFY] Attendance marked ABSENT — studentId={}, classId={}, sessionDate={}",
                event.studentId(), event.classId(), event.sessionDate());
        // TODO (RF-32): optionally notify student of absence (Postmark)
    }

    @Async
    @EventListener
    public void onAttendanceMarkedPresentNoHours(AttendanceMarkedPresentNoHours event) {
        log.info("[NOTIFY] Attendance marked PRESENT (no hours) — studentId={}, classId={}, sessionDate={}. " +
                 "Student has insufficient membership hours.",
                event.studentId(), event.classId(), event.sessionDate());
        // TODO (RF-32): notify student that they attended but hours could not be deducted (Postmark)
    }

    @Async
    @EventListener
    public void onAttendanceCorrected(AttendanceCorrected event) {
        log.info("[NOTIFY] Attendance corrected — registrationId={}, studentId={}, {} -> {}, reason={}",
                event.registrationId(), event.studentId(),
                event.previousStatus(), event.newStatus(), event.reason());
        // TODO (RF-32): notify student of attendance correction (Postmark)
    }
}
