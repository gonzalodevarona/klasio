package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.port.input.ListEligibleStudentsUseCase;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassSummaryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.EligibleStudentLookupPort;
import com.klasio.attendance.domain.port.EligibleStudentLookupPort.EligibleStudentView;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import com.klasio.shared.infrastructure.exception.MarkingWindowException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListEligibleStudentsService implements ListEligibleStudentsUseCase {

    private final ClassDetailsPort classDetailsPort;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRegistrationRepository registrationRepository;
    private final ProfessorIdLookupPort professorIdLookupPort;
    private final EligibleStudentLookupPort eligibleStudentLookupPort;

    public ListEligibleStudentsService(ClassDetailsPort classDetailsPort,
                                        ClassSessionRepository classSessionRepository,
                                        AttendanceRegistrationRepository registrationRepository,
                                        ProfessorIdLookupPort professorIdLookupPort,
                                        EligibleStudentLookupPort eligibleStudentLookupPort) {
        this.classDetailsPort = classDetailsPort;
        this.classSessionRepository = classSessionRepository;
        this.registrationRepository = registrationRepository;
        this.professorIdLookupPort = professorIdLookupPort;
        this.eligibleStudentLookupPort = eligibleStudentLookupPort;
    }

    @Override
    public List<EligibleStudentView> execute(UUID tenantId,
                                              UUID classId,
                                              LocalDate sessionDate,
                                              LocalTime startTime,
                                              String nameFilter,
                                              String role,
                                              UUID actorUserId,
                                              UUID programIdFromJwt) {
        // 1. Load class summary — validates existence
        ClassSummaryView classSummary = classDetailsPort.findClassSummary(tenantId, classId)
                .orElseThrow(() -> new ClassNotFoundException("Class not found: " + classId));

        // 2. RBAC scope check
        if ("PROFESSOR".equals(role)) {
            UUID resolvedProfessorId = professorIdLookupPort
                    .findProfessorIdByUserId(tenantId, actorUserId)
                    .orElseThrow(() -> new AccessDeniedException(
                            "Professor not found for user: " + actorUserId));
            if (!resolvedProfessorId.equals(classSummary.professorId())) {
                throw new AccessDeniedException("Professor is not assigned to this class");
            }
        } else if ("MANAGER".equals(role)) {
            if (!programIdFromJwt.equals(classSummary.programId())) {
                throw new AccessDeniedException("Manager does not belong to this class's program");
            }
        }
        // ADMIN / SUPERADMIN: no additional restriction

        // 3. Marking window check — allow from (sessionStart - BEFORE) to (sessionStart + 24h)
        //    Uses a permissive upper bound so the picker stays available throughout the session.
        ZonedDateTime sessionStart = LocalDateTime.of(sessionDate, startTime)
                .atZone(AttendanceTimeConstants.TENANT_ZONE);
        ZonedDateTime now = ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE);

        ZonedDateTime windowOpen  = sessionStart.minusMinutes(AttendanceTimeConstants.MARKING_WINDOW_MINUTES_BEFORE);
        ZonedDateTime windowClose = sessionStart.plusHours(24); // permissive upper bound for picker

        if (now.isBefore(windowOpen) || now.isAfter(windowClose)) {
            throw new MarkingWindowException(
                    "Student picker is only available from "
                            + AttendanceTimeConstants.MARKING_WINDOW_MINUTES_BEFORE
                            + " minutes before the session starts until end of session day");
        }

        // 4. Build exclude set from active registrations for this session (if session exists)
        Set<UUID> excludeStudentIds = Collections.emptySet();
        Optional<ClassSession> sessionOpt = classSessionRepository.findByClassAndDate(tenantId, classId, sessionDate);
        if (sessionOpt.isPresent()) {
            UUID sessionId = sessionOpt.get().getId().value();
            excludeStudentIds = registrationRepository.findActiveStudentIdsBySession(tenantId, sessionId);
        }

        // 5. Compute limit: 50 without filter, 20 with filter
        int limit = (nameFilter == null) ? 50 : 20;

        // 6. Load class level — needed to filter eligible students by level
        //    Use findForRegistration which carries the level field
        String level = classDetailsPort.findForRegistration(tenantId, classId)
                .map(ClassDetailsPort.ClassRegistrationView::level)
                .orElse(null); // if somehow missing, let port handle null gracefully

        // 7. Delegate to the lookup port
        return eligibleStudentLookupPort.findEligible(
                tenantId,
                classSummary.programId(),
                level,
                1,
                nameFilter,
                excludeStudentIds,
                limit);
    }
}
