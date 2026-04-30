package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.AvailableSessionView;
import com.klasio.attendance.application.port.input.GetAvailableSessionsUseCase;
import com.klasio.attendance.domain.port.EnrollmentLookupPort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class GetMyAvailableSessionsService {

    private final EnrollmentLookupPort enrollmentLookupPort;
    private final GetAvailableSessionsUseCase getAvailableSessionsUseCase;

    public GetMyAvailableSessionsService(EnrollmentLookupPort enrollmentLookupPort,
                                         GetAvailableSessionsUseCase getAvailableSessionsUseCase) {
        this.enrollmentLookupPort = enrollmentLookupPort;
        this.getAvailableSessionsUseCase = getAvailableSessionsUseCase;
    }

    public List<AvailableSessionView> execute(UUID tenantId, UUID studentId,
                                              LocalDate from, LocalDate to, boolean includeFull) {
        List<EnrollmentLookupPort.StudentEnrollmentView> enrollments =
                enrollmentLookupPort.findAllActiveEnrollmentsForStudent(tenantId, studentId);

        return enrollments.stream()
                .flatMap(enrollment -> getAvailableSessionsUseCase
                        .execute(tenantId, studentId, enrollment.programId(), from, to, includeFull)
                        .stream())
                .sorted(Comparator.comparing(AvailableSessionView::sessionDate)
                        .thenComparing(AvailableSessionView::startTime))
                .toList();
    }
}
