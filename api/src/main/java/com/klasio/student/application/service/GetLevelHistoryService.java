package com.klasio.student.application.service;

import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.student.application.dto.LevelHistoryDetail;
import com.klasio.student.application.port.input.GetLevelHistoryUseCase;
import com.klasio.student.domain.port.LevelHistoryRepository;
import com.klasio.student.domain.port.StudentEnrollmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetLevelHistoryService implements GetLevelHistoryUseCase {

    private final StudentEnrollmentRepository enrollmentRepository;
    private final LevelHistoryRepository levelHistoryRepository;

    public GetLevelHistoryService(StudentEnrollmentRepository enrollmentRepository,
                                  LevelHistoryRepository levelHistoryRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.levelHistoryRepository = levelHistoryRepository;
    }

    @Override
    public Page<LevelHistoryDetail> execute(UUID tenantId, UUID enrollmentId, int page, int size) {
        enrollmentRepository.findById(tenantId, enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(
                        "Enrollment with id '%s' not found".formatted(enrollmentId)));

        return levelHistoryRepository.findByEnrollmentId(tenantId, enrollmentId, page, size)
                .map(LevelHistoryDetail::fromDomain);
    }
}
