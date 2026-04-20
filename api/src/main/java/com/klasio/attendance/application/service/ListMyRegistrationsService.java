package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.AttendanceRegistrationView;
import com.klasio.attendance.application.port.input.ListMyRegistrationsUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ListMyRegistrationsService implements ListMyRegistrationsUseCase {

    private final AttendanceRegistrationRepository registrationRepository;
    private final ClassDetailsPort classDetailsPort;
    private final ClassSessionRepository sessionRepository;

    public ListMyRegistrationsService(AttendanceRegistrationRepository registrationRepository,
                                      ClassDetailsPort classDetailsPort,
                                      ClassSessionRepository sessionRepository) {
        this.registrationRepository = registrationRepository;
        this.classDetailsPort = classDetailsPort;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public Page<AttendanceRegistrationView> execute(UUID tenantId, UUID studentId,
                                                    LocalDate from, LocalDate to,
                                                    AttendanceRegistrationStatus status,
                                                    UUID programId,
                                                    Pageable pageable) {
        Page<AttendanceRegistration> page =
                registrationRepository.findByStudent(tenantId, studentId, from, to, status, programId, pageable);

        // Batch-resolve class names for all unique classIds on this page
        Map<UUID, String> nameCache = new HashMap<>();
        page.forEach(r -> nameCache.computeIfAbsent(r.getClassId(),
                id -> classDetailsPort.findClassName(tenantId, id).orElse("Unknown class")));

        // Batch-resolve session status/alertReason for all unique non-null sessionIds on this page
        List<UUID> sessionIds = page.getContent().stream()
                .map(AttendanceRegistration::getSessionId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, ClassSession> sessionCache = sessionRepository.findByIds(tenantId, sessionIds)
                .stream()
                .collect(Collectors.toMap(s -> s.getId().value(), s -> s));

        return page.map(r -> toView(r,
                nameCache.getOrDefault(r.getClassId(), "Unknown class"),
                r.getSessionId() != null ? sessionCache.get(r.getSessionId()) : null));
    }

    private AttendanceRegistrationView toView(AttendanceRegistration r, String className,
                                               ClassSession session) {
        String sessionStatus = session != null ? session.getStatus().name() : null;
        String sessionAlertReason = session != null ? session.getAlertReason() : null;

        return new AttendanceRegistrationView(
                r.getId().value(),
                r.getSessionId(),
                r.getClassId(),
                className,
                r.getStudentId(),
                r.getSessionDate(),
                r.getSessionStartTime(),
                r.getSessionEndTime(),
                r.getLevelAtRegistration(),
                r.getIntendedHours(),
                r.getStatus().name(),
                r.getCreatedAt(),
                r.getCancellationReason(),
                sessionStatus,
                sessionAlertReason
        );
    }
}
