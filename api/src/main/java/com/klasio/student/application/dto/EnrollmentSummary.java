package com.klasio.student.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EnrollmentSummary(
        UUID id,
        UUID studentId,
        String studentName,
        UUID programId,
        String programName,
        String level,
        LocalDate enrollmentDate,
        String status
) {}
