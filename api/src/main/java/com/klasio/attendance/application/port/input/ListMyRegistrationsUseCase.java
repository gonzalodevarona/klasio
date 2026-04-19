package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.AttendanceRegistrationView;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface ListMyRegistrationsUseCase {
    Page<AttendanceRegistrationView> execute(UUID tenantId, UUID studentId,
                                             LocalDate from, LocalDate to,
                                             AttendanceRegistrationStatus status,
                                             UUID programId,
                                             Pageable pageable);
}
