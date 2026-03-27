package com.klasio.student.application.service;

import com.klasio.program.domain.port.ProgramRepository;
import com.klasio.student.application.dto.EnrollmentSummary;
import com.klasio.student.application.port.input.ListEnrollmentsUseCase;
import com.klasio.student.domain.model.StudentEnrollment;
import com.klasio.student.domain.port.StudentEnrollmentRepository;
import com.klasio.student.domain.port.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ListEnrollmentsService implements ListEnrollmentsUseCase {

    private final StudentEnrollmentRepository enrollmentRepository;
    private final ProgramRepository programRepository;
    private final StudentRepository studentRepository;

    public ListEnrollmentsService(StudentEnrollmentRepository enrollmentRepository,
                                  ProgramRepository programRepository,
                                  StudentRepository studentRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.programRepository = programRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    public Page<EnrollmentSummary> byProgram(UUID tenantId, UUID programId, int page, int size, String level, String status) {
        return enrollmentRepository.findByProgramId(tenantId, programId, page, size, level, status)
                .map(enrollment -> toSummaryWithStudent(tenantId, enrollment));
    }

    @Override
    public Page<EnrollmentSummary> byStudent(UUID tenantId, UUID studentId, int page, int size, String status) {
        return enrollmentRepository.findByStudentId(tenantId, studentId, page, size, status)
                .map(enrollment -> toSummaryWithProgram(tenantId, enrollment));
    }

    private EnrollmentSummary toSummaryWithProgram(UUID tenantId, StudentEnrollment enrollment) {
        String programName = programRepository.findById(tenantId, enrollment.getProgramId())
                .map(p -> p.getName())
                .orElse(null);
        return new EnrollmentSummary(
                enrollment.getId().value(),
                enrollment.getStudentId(),
                null,
                enrollment.getProgramId(),
                programName,
                enrollment.getLevel().name(),
                enrollment.getEnrollmentDate(),
                enrollment.getStatus()
        );
    }

    private EnrollmentSummary toSummaryWithStudent(UUID tenantId, StudentEnrollment enrollment) {
        String studentName = studentRepository.findById(tenantId, enrollment.getStudentId())
                .map(s -> s.getFirstName() + " " + s.getLastName())
                .orElse(null);
        return new EnrollmentSummary(
                enrollment.getId().value(),
                enrollment.getStudentId(),
                studentName,
                enrollment.getProgramId(),
                null,
                enrollment.getLevel().name(),
                enrollment.getEnrollmentDate(),
                enrollment.getStatus()
        );
    }
}
