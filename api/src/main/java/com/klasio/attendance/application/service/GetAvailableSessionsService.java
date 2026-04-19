package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.AvailableSessionView;
import com.klasio.attendance.application.port.input.GetAvailableSessionsUseCase;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.EnrollmentLookupPort;
import com.klasio.attendance.domain.port.EnrollmentLookupPort.EnrollmentView;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetAvailableSessionsService implements GetAvailableSessionsUseCase {

    private final EnrollmentLookupPort enrollmentLookupPort;
    private final ClassDetailsPort classDetailsPort;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRegistrationRepository registrationRepository;

    public GetAvailableSessionsService(EnrollmentLookupPort enrollmentLookupPort,
                                       ClassDetailsPort classDetailsPort,
                                       ClassSessionRepository classSessionRepository,
                                       AttendanceRegistrationRepository registrationRepository) {
        this.enrollmentLookupPort = enrollmentLookupPort;
        this.classDetailsPort = classDetailsPort;
        this.classSessionRepository = classSessionRepository;
        this.registrationRepository = registrationRepository;
    }

    @Override
    public List<AvailableSessionView> execute(UUID tenantId, UUID studentId, UUID programId,
                                              LocalDate from, LocalDate to, boolean includeFull) {
        // 1. Validate window ≤ 30 days
        long daysBetween = ChronoUnit.DAYS.between(from, to);
        if (daysBetween > AttendanceTimeConstants.MAX_AVAILABLE_SESSIONS_WINDOW_DAYS) {
            throw new IllegalArgumentException(
                    "Date window must not exceed " + AttendanceTimeConstants.MAX_AVAILABLE_SESSIONS_WINDOW_DAYS
                            + " days, got: " + daysBetween);
        }

        // 2. Find the student's enrollment in the program — determines level
        EnrollmentView programEnrollment = enrollmentLookupPort
                .findActiveEnrollmentInProgram(tenantId, studentId, programId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Student " + studentId + " is not enrolled in program " + programId));

        String level = programEnrollment.level();

        // 3. Fetch active classes in program at student's level
        List<ClassRegistrationView> classes = classDetailsPort.findActiveByProgramAndLevel(tenantId, programId, level);
        if (classes.isEmpty()) {
            return List.of();
        }

        // 4. Expand schedule entries into concrete (classId, date, startTime, endTime) tuples
        List<SessionTuple> tuples = expandSchedules(classes, from, to);
        if (tuples.isEmpty()) {
            return List.of();
        }

        // 5. Bulk fetch materialized sessions for capacity/status enrichment
        List<UUID> classIds = classes.stream().map(ClassRegistrationView::id).collect(Collectors.toList());
        List<ClassSession> materializedSessions = classSessionRepository.findByClassIdsAndDateRange(
                tenantId, classIds, from, to);

        Map<String, ClassSession> sessionByKey = materializedSessions.stream()
                .collect(Collectors.toMap(
                        s -> sessionKey(s.getClassId(), s.getSessionDate(), s.getStartTime()),
                        s -> s
                ));

        // 6. Fetch session IDs where student already has a REGISTERED registration
        List<UUID> materializedSessionIds = materializedSessions.stream()
                .map(s -> s.getId().value())
                .collect(Collectors.toList());
        Set<UUID> alreadyRegisteredIds = materializedSessionIds.isEmpty()
                ? Set.of()
                : registrationRepository.findRegisteredSessionIds(tenantId, studentId, materializedSessionIds);

        // 7. Time thresholds
        ZonedDateTime now    = ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE);
        ZonedDateTime cutoff = now.plusMinutes(AttendanceTimeConstants.REGISTRATION_CUTOFF_MINUTES);

        // 8. Build class lookup map
        Map<UUID, ClassRegistrationView> classMap = classes.stream()
                .collect(Collectors.toMap(ClassRegistrationView::id, c -> c));

        // 9. Filter and build result
        List<AvailableSessionView> result = new ArrayList<>();

        for (SessionTuple tuple : tuples) {
            ZonedDateTime sessionStart = LocalDateTime.of(tuple.sessionDate(), tuple.startTime())
                    .atZone(AttendanceTimeConstants.TENANT_ZONE);

            // Drop sessions that have already started (past sessions are useless for registration)
            if (!sessionStart.isAfter(now)) {
                continue;
            }

            // Registration is only open when the session start is still beyond the cutoff window
            boolean registrationOpen = sessionStart.isAfter(cutoff);

            String key = sessionKey(tuple.classId(), tuple.sessionDate(), tuple.startTime());
            ClassSession materialized = sessionByKey.get(key);

            // Filter: CANCELLED
            if (materialized != null && materialized.getStatus() == ClassSessionStatus.CANCELLED) {
                continue;
            }

            int currentCapacity = materialized != null ? materialized.getCurrentCapacity() : 0;
            UUID sessionId = materialized != null ? materialized.getId().value() : null;
            String status = materialized != null ? materialized.getStatus().name() : "SCHEDULED";

            ClassRegistrationView classView = classMap.get(tuple.classId());
            int maxStudents = classView.maxStudents();

            // Filter: already registered
            if (sessionId != null && alreadyRegisteredIds.contains(sessionId)) {
                continue;
            }

            // Filter: full unless includeFull=true
            if (!includeFull && currentCapacity >= maxStudents) {
                continue;
            }

            result.add(new AvailableSessionView(
                    tuple.classId(),
                    classView.className(),
                    sessionId,
                    tuple.sessionDate(),
                    tuple.startTime(),
                    tuple.endTime(),
                    level,
                    programId,
                    currentCapacity,
                    maxStudents,
                    status,
                    registrationOpen
            ));
        }

        // 10. Sort by date + time
        result.sort(Comparator.comparing(AvailableSessionView::sessionDate)
                .thenComparing(AvailableSessionView::startTime));

        return result;
    }

    private List<SessionTuple> expandSchedules(List<ClassRegistrationView> classes,
                                               LocalDate from, LocalDate to) {
        List<SessionTuple> tuples = new ArrayList<>();
        for (ClassRegistrationView cls : classes) {
            for (ScheduleEntryView entry : cls.scheduleEntries()) {
                if ("ONE_TIME".equals(cls.type())) {
                    LocalDate specificDate = entry.specificDate();
                    if (specificDate != null && !specificDate.isBefore(from) && !specificDate.isAfter(to)) {
                        tuples.add(new SessionTuple(cls.id(), specificDate, entry.startTime(), entry.endTime()));
                    }
                } else {
                    // RECURRING: walk days in window matching dayOfWeek
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

    private String sessionKey(UUID classId, LocalDate date, LocalTime startTime) {
        return classId + "|" + date + "|" + startTime;
    }

    private record SessionTuple(UUID classId, LocalDate sessionDate, LocalTime startTime, LocalTime endTime) {}
}
