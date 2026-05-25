package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.ClassSessionRosterView;
import com.klasio.attendance.application.dto.ClassSessionRosterView.RegistrantView;
import com.klasio.attendance.application.port.input.ListClassSessionRosterUseCase;
import com.klasio.attendance.application.util.ClassScheduleExpander;
import com.klasio.attendance.application.util.ClassScheduleExpander.SessionTuple;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassSummaryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.DropInAttendeeLookupPort;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import com.klasio.membership.domain.port.StudentNamePort;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ListClassSessionRosterService implements ListClassSessionRosterUseCase {

    private static final int MAX_WINDOW_DAYS = 30;

    private final ClassDetailsPort classDetailsPort;
    private final AttendanceRegistrationRepository registrationRepository;
    private final ClassSessionRepository sessionRepository;
    private final StudentNamePort studentNamePort;
    private final ProfessorIdLookupPort professorIdLookupPort;
    private final DropInAttendeeLookupPort dropInLookupPort;

    public ListClassSessionRosterService(ClassDetailsPort classDetailsPort,
                                          AttendanceRegistrationRepository registrationRepository,
                                          ClassSessionRepository sessionRepository,
                                          StudentNamePort studentNamePort,
                                          ProfessorIdLookupPort professorIdLookupPort,
                                          DropInAttendeeLookupPort dropInLookupPort) {
        this.classDetailsPort = classDetailsPort;
        this.registrationRepository = registrationRepository;
        this.sessionRepository = sessionRepository;
        this.studentNamePort = studentNamePort;
        this.professorIdLookupPort = professorIdLookupPort;
        this.dropInLookupPort = dropInLookupPort;
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

        // 2. Load class summary for RBAC scope check
        ClassSummaryView classView = classDetailsPort
                .findClassSummary(tenantId, classId)
                .orElseThrow(() -> new ClassNotFoundException("Class not found: " + classId));

        // 3. RBAC scope guard
        enforceScope(role, userId, tenantId, classView, programIdFromJwt);

        // 4. Load full class view (schedule + type needed for expansion)
        ClassRegistrationView classDetail = classDetailsPort
                .findForRegistration(tenantId, classId)
                .orElseThrow(() -> new ClassNotFoundException("Class not found: " + classId));

        // 5. Expand schedule into expected session tuples within [from, to]
        List<SessionTuple> tuples = ClassScheduleExpander.expand(List.of(classDetail), from, to);
        if (tuples.isEmpty()) {
            return List.of();
        }

        // 6. Load all registrations in window
        List<AttendanceRegistration> registrations =
                registrationRepository.findByClassAndDateRange(tenantId, classId, from, to);

        // 7. Bucket registrations by (date, startTime, endTime)
        boolean exposeCreatedBy = "ADMIN".equals(role) || "SUPERADMIN".equals(role) || "MANAGER".equals(role);

        // Only look up student names for non-drop-in registrations
        Set<UUID> studentIds = registrations.stream()
                .filter(r -> r.getStudentId() != null)
                .map(AttendanceRegistration::getStudentId)
                .collect(Collectors.toSet());

        // Bulk-fetch drop-in attendee info
        Set<UUID> dropInAttendeeIds = registrations.stream()
                .filter(r -> r.getDropInAttendeeId() != null)
                .map(AttendanceRegistration::getDropInAttendeeId)
                .collect(Collectors.toSet());

        Set<UUID> dropInPaymentIds = registrations.stream()
                .filter(r -> r.getDropInPaymentId() != null)
                .map(AttendanceRegistration::getDropInPaymentId)
                .collect(Collectors.toSet());

        Map<UUID, String> nameCache = new HashMap<>();
        for (UUID sid : studentIds) {
            nameCache.put(sid, studentNamePort.findFullName(sid, tenantId).orElse("Unknown"));
        }

        Map<UUID, DropInAttendeeLookupPort.DropInRosterEntry> dropInCache =
                dropInLookupPort.findByAttendeeIds(tenantId, dropInAttendeeIds);
        Map<UUID, BigDecimal> paymentCache =
                dropInLookupPort.findAmountsByPaymentIds(tenantId, dropInPaymentIds);

        Map<String, List<RegistrantView>> registrantsByKey = new HashMap<>();
        for (AttendanceRegistration r : registrations) {
            String key = sessionKey(r.getSessionDate(), r.getSessionStartTime(), r.getSessionEndTime());

            UUID dropInId = r.getDropInAttendeeId();
            DropInAttendeeLookupPort.DropInRosterEntry dropInEntry = dropInId != null ? dropInCache.get(dropInId) : null;
            BigDecimal paymentAmount = r.getDropInPaymentId() != null ? paymentCache.get(r.getDropInPaymentId()) : null;

            RegistrantView view = new RegistrantView(
                    r.getId().value(),
                    r.getStudentId(),
                    r.getStudentId() != null ? nameCache.getOrDefault(r.getStudentId(), "Unknown") : null,
                    r.getLevelAtRegistration(),
                    r.getIntendedHours(),
                    r.getStatus().name(),
                    exposeCreatedBy ? r.getCreatedBy() : null,
                    dropInId,
                    dropInEntry != null ? dropInEntry.fullName() : null,
                    dropInEntry != null ? dropInEntry.phone() : null,
                    paymentAmount
            );
            registrantsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(view);
        }

        // 8. Build one view per tuple, enriching with materialized session status
        List<ClassSessionRosterView> result = new ArrayList<>();
        for (SessionTuple tuple : tuples) {
            Optional<ClassSession> cs =
                    sessionRepository.findByClassAndDate(tenantId, classId, tuple.sessionDate());
            String sessionStatus      = cs.map(s -> s.getStatus().name()).orElse("SCHEDULED");
            String alertReason        = cs.map(ClassSession::getAlertReason).orElse(null);
            String cancellationReason = cs.map(ClassSession::getCancellationReason).orElse(null);

            String key = sessionKey(tuple.sessionDate(), tuple.startTime(), tuple.endTime());
            List<RegistrantView> registrants = registrantsByKey.getOrDefault(key, List.of());

            result.add(new ClassSessionRosterView(
                    tuple.sessionDate(),
                    tuple.startTime(),
                    tuple.endTime(),
                    sessionStatus,
                    alertReason,
                    cancellationReason,
                    List.copyOf(registrants)
            ));
        }

        // 9. Sort by date then startTime
        result.sort(Comparator.comparing(ClassSessionRosterView::sessionDate)
                .thenComparing(ClassSessionRosterView::startTime));

        return result;
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

    private String sessionKey(LocalDate date, LocalTime startTime, LocalTime endTime) {
        return date + "|" + startTime + "|" + endTime;
    }
}
