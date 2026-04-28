package com.klasio.attendance.application.service;

import com.klasio.attendance.AttendanceTimeConstants;
import com.klasio.attendance.application.dto.MarkAttendanceCommand;
import com.klasio.attendance.application.dto.MarkAttendanceResult;
import com.klasio.attendance.application.port.input.MarkAttendanceUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.MembershipHoursPort;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import com.klasio.membership.application.dto.DeductHoursCommand;
import com.klasio.membership.application.port.input.DeductHoursUseCase;
import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import com.klasio.shared.infrastructure.exception.MarkingWindowException;
import com.klasio.shared.infrastructure.exception.RegistrationNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class MarkAttendanceService implements MarkAttendanceUseCase {

    private final ClassDetailsPort classDetailsPort;
    private final AttendanceRegistrationRepository registrationRepository;
    private final ProfessorIdLookupPort professorIdLookupPort;
    private final MembershipHoursPort membershipHoursPort;
    private final DeductHoursUseCase deductHoursUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public MarkAttendanceService(ClassDetailsPort classDetailsPort,
                                 AttendanceRegistrationRepository registrationRepository,
                                 ProfessorIdLookupPort professorIdLookupPort,
                                 MembershipHoursPort membershipHoursPort,
                                 DeductHoursUseCase deductHoursUseCase,
                                 ApplicationEventPublisher eventPublisher) {
        this.classDetailsPort = classDetailsPort;
        this.registrationRepository = registrationRepository;
        this.professorIdLookupPort = professorIdLookupPort;
        this.membershipHoursPort = membershipHoursPort;
        this.deductHoursUseCase = deductHoursUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public MarkAttendanceResult execute(MarkAttendanceCommand command) {
        UUID tenantId = command.tenantId();
        UUID classId = command.classId();
        String actorRole = command.actorRole();
        UUID actorId = command.actorId();

        // 1. Load class summary
        ClassDetailsPort.ClassSummaryView classView = classDetailsPort.findClassSummary(tenantId, classId)
                .orElseThrow(() -> new ClassNotFoundException("Class not found: " + classId));

        // 2. RBAC scope check (pre-registration-load)
        if ("PROFESSOR".equals(actorRole)) {
            UUID resolvedProfessorId = professorIdLookupPort.findProfessorIdByUserId(tenantId, actorId)
                    .orElseThrow(() -> new AccessDeniedException("Professor not found for user: " + actorId));
            if (!resolvedProfessorId.equals(classView.professorId())) {
                throw new AccessDeniedException("Professor is not assigned to this class");
            }
        } else if ("MANAGER".equals(actorRole)) {
            if (!command.programIdFromJwt().equals(classView.programId())) {
                throw new AccessDeniedException("Manager does not belong to this class's program");
            }
        }
        // ADMIN / SUPERADMIN: no RBAC restriction but still subject to time window

        // 3. Load all registrations for this session
        List<AttendanceRegistration> registrations = registrationRepository.findByClassAndDateRange(
                tenantId, classId, command.sessionDate(), command.sessionDate());

        Map<UUID, AttendanceRegistration> byId = registrations.stream()
                .collect(Collectors.toMap(r -> r.getId().value(), Function.identity()));

        // 4. Time-window check — applies to ALL roles.
        //    Window: [sessionStart - 20 min, sessionEnd + 10 min]
        ZonedDateTime sessionStart = LocalDateTime
                .of(command.sessionDate(), command.startTime())
                .atZone(AttendanceTimeConstants.TENANT_ZONE);
        ZonedDateTime now = ZonedDateTime.now(AttendanceTimeConstants.TENANT_ZONE);

        ZonedDateTime windowOpen  = sessionStart.minusMinutes(AttendanceTimeConstants.MARKING_WINDOW_MINUTES_BEFORE);
        ZonedDateTime windowClose;

        if (!registrations.isEmpty()) {
            AttendanceRegistration sample = registrations.get(0);
            windowClose = LocalDateTime
                    .of(sample.getSessionDate(), sample.getSessionEndTime())
                    .atZone(AttendanceTimeConstants.TENANT_ZONE)
                    .plusMinutes(AttendanceTimeConstants.MARKING_WINDOW_MINUTES_AFTER);
        } else {
            // No registrations yet — close window cannot be derived; allow until startTime + 10 min as a safe fallback.
            windowClose = sessionStart.plusMinutes(AttendanceTimeConstants.MARKING_WINDOW_MINUTES_AFTER);
        }

        if (now.isBefore(windowOpen) || now.isAfter(windowClose)) {
            throw new MarkingWindowException(
                    "Attendance can only be marked from 20 minutes before the session starts until 10 minutes after it ends");
        }

        // 5. Process each mark entry
        Instant nowInstant = Instant.now();
        List<DomainEvent> allEvents = new ArrayList<>();
        List<MarkAttendanceResult.MarkedRegistration> results = new ArrayList<>();

        for (MarkAttendanceCommand.MarkEntry entry : command.marks()) {
            AttendanceRegistration reg = byId.get(entry.registrationId());
            if (reg == null) {
                throw new RegistrationNotFoundException(entry.registrationId());
            }

            // Idempotent: already marked → include as-is
            if (reg.getStatus() != AttendanceRegistrationStatus.REGISTERED) {
                results.add(new MarkAttendanceResult.MarkedRegistration(
                        entry.registrationId(), reg.getStatus().name(), false));
                continue;
            }

            boolean noHoursWarning = false;

            if ("PRESENT".equals(entry.mark())) {
                Optional<MembershipHoursPort.ActiveMembershipView> membershipOpt =
                        membershipHoursPort.findActiveForStudentInProgram(tenantId, reg.getStudentId(), classView.programId());

                boolean hasActiveMembership = membershipOpt.isPresent();
                boolean isUnlimited = hasActiveMembership && membershipOpt.get().unlimited();
                boolean canDeduct = hasActiveMembership && !isUnlimited &&
                        membershipOpt.get().availableHours() >= reg.getIntendedHours();

                if (isUnlimited) {
                    // UNLIMITED: attendance is free — no hour deduction, no warning
                    reg.markPresent(actorId, nowInstant);
                } else if (canDeduct) {
                    deductHoursUseCase.execute(new DeductHoursCommand(
                            tenantId,
                            membershipOpt.get().membershipId(),
                            reg.getIntendedHours(),
                            actorId,
                            actorRole));
                    reg.markPresent(actorId, nowInstant);
                } else {
                    reg.markPresentNoHours(actorId, nowInstant);
                    noHoursWarning = true;
                }
            } else if ("ABSENT".equals(entry.mark())) {
                reg.markAbsent(actorId, nowInstant);
            }

            registrationRepository.save(reg);
            allEvents.addAll(reg.getDomainEvents());
            reg.clearDomainEvents();

            results.add(new MarkAttendanceResult.MarkedRegistration(
                    entry.registrationId(), reg.getStatus().name(), noHoursWarning));
        }

        // 6. Publish all domain events
        allEvents.forEach(eventPublisher::publishEvent);

        return new MarkAttendanceResult(results);
    }
}
