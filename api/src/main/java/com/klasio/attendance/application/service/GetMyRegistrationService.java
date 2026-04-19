package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.AttendanceRegistrationView;
import com.klasio.attendance.application.port.input.GetMyRegistrationUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetMyRegistrationService implements GetMyRegistrationUseCase {

    private final AttendanceRegistrationRepository registrationRepository;
    private final ClassDetailsPort classDetailsPort;

    public GetMyRegistrationService(AttendanceRegistrationRepository registrationRepository,
                                    ClassDetailsPort classDetailsPort) {
        this.registrationRepository = registrationRepository;
        this.classDetailsPort = classDetailsPort;
    }

    @Override
    public AttendanceRegistrationView execute(UUID tenantId, UUID studentId, UUID registrationId) {
        AttendanceRegistration registration = registrationRepository.findById(tenantId, registrationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Registration not found: " + registrationId));

        // Ownership check
        if (!registration.getStudentId().equals(studentId)) {
            throw new AccessDeniedException("Registration does not belong to this student");
        }

        String className = classDetailsPort.findClassName(tenantId, registration.getClassId())
                .orElse("Unknown class");

        return new AttendanceRegistrationView(
                registration.getId().value(),
                registration.getSessionId(),
                registration.getClassId(),
                className,
                registration.getStudentId(),
                registration.getSessionDate(),
                registration.getSessionStartTime(),
                registration.getSessionEndTime(),
                registration.getLevelAtRegistration(),
                registration.getIntendedHours(),
                registration.getStatus().name(),
                registration.getCreatedAt()
        );
    }
}
