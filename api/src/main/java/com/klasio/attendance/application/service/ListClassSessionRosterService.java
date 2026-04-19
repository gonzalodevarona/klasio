package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.ClassSessionRosterView;
import com.klasio.attendance.application.dto.ClassSessionRosterView.RegistrantView;
import com.klasio.attendance.application.port.input.ListClassSessionRosterUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassSummaryView;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import com.klasio.membership.domain.port.StudentNamePort;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ListClassSessionRosterService implements ListClassSessionRosterUseCase {

    private static final int MAX_WINDOW_DAYS = 30;

    private final ClassDetailsPort classDetailsPort;
    private final AttendanceRegistrationRepository registrationRepository;
    private final StudentNamePort studentNamePort;
    private final ProfessorIdLookupPort professorIdLookupPort;

    public ListClassSessionRosterService(ClassDetailsPort classDetailsPort,
                                          AttendanceRegistrationRepository registrationRepository,
                                          StudentNamePort studentNamePort,
                                          ProfessorIdLookupPort professorIdLookupPort) {
        this.classDetailsPort = classDetailsPort;
        this.registrationRepository = registrationRepository;
        this.studentNamePort = studentNamePort;
        this.professorIdLookupPort = professorIdLookupPort;
    }

    @Override
    public List<ClassSessionRosterView> execute(UUID tenantId, UUID classId,
                                                 LocalDate from, LocalDate to,
                                                 String role, UUID userId,
                                                 UUID programIdFromJwt) {
        // 1. Validate window
        if (ChronoUnit.DAYS.between(from, to) > MAX_WINDOW_DAYS) {
            throw new IllegalArgumentException(
                    "Date range must not exceed " + MAX_WINDOW_DAYS + " days");
        }

        // 2. Load class summary for scope check
        ClassSummaryView classView = classDetailsPort
                .findClassSummary(tenantId, classId)
                .orElseThrow(() -> new ClassNotFoundException("Class not found: " + classId));

        // 3. RBAC scope guard
        enforceScope(role, userId, tenantId, classView, programIdFromJwt);

        // 4. Load registrations
        List<AttendanceRegistration> registrations =
                registrationRepository.findByClassAndDateRange(tenantId, classId, from, to);

        if (registrations.isEmpty()) {
            return List.of();
        }

        // 5. Batch-resolve student names (one call per unique studentId)
        Set<UUID> studentIds = registrations.stream()
                .map(AttendanceRegistration::getStudentId)
                .collect(Collectors.toSet());

        Map<UUID, String> nameCache = new java.util.HashMap<>();
        for (UUID sid : studentIds) {
            nameCache.put(sid, studentNamePort.findFullName(sid, tenantId).orElse("Unknown"));
        }

        // 6. Group by (sessionDate, startTime, endTime) preserving date+time order
        LinkedHashMap<SessionKey, List<RegistrantView>> grouped = new LinkedHashMap<>();
        for (AttendanceRegistration r : registrations) {
            SessionKey key = new SessionKey(r.getSessionDate(), r.getSessionStartTime(), r.getSessionEndTime());
            grouped.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new RegistrantView(
                            r.getId().value(),
                            r.getStudentId(),
                            nameCache.getOrDefault(r.getStudentId(), "Unknown"),
                            r.getLevelAtRegistration(),
                            r.getIntendedHours(),
                            r.getStatus().name()
                    ));
        }

        // 7. Build result (already sorted by date+time because the SQL query orders by session_date, start_time)
        return grouped.entrySet().stream()
                .map(e -> new ClassSessionRosterView(
                        e.getKey().date(),
                        e.getKey().startTime(),
                        e.getKey().endTime(),
                        List.copyOf(e.getValue())
                ))
                .toList();
    }

    private void enforceScope(String role, UUID userId, UUID tenantId,
                               ClassSummaryView classView, UUID programIdFromJwt) {
        switch (role) {
            case "SUPERADMIN", "ADMIN" -> { /* full access */ }
            case "MANAGER" -> {
                if (programIdFromJwt == null || !programIdFromJwt.equals(classView.programId())) {
                    throw new AccessDeniedException(
                            "You can only view registrations for classes in your program");
                }
            }
            case "PROFESSOR" -> {
                UUID professorId = professorIdLookupPort
                        .findProfessorIdByUserId(tenantId, userId)
                        .orElseThrow(() -> new AccessDeniedException(
                                "No professor profile found for this user"));
                if (!professorId.equals(classView.professorId())) {
                    throw new AccessDeniedException(
                            "You are not the assigned professor for this class");
                }
            }
            default -> throw new AccessDeniedException("Role not authorized to view rosters");
        }
    }

    private record SessionKey(LocalDate date, LocalTime startTime, LocalTime endTime) {}
}
