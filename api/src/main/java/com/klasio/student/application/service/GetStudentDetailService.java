package com.klasio.student.application.service;

import com.klasio.shared.domain.port.UserDisplayNamePort;
import com.klasio.shared.infrastructure.exception.StudentNotFoundException;
import com.klasio.student.application.dto.StudentDetail;
import com.klasio.student.application.port.input.GetStudentDetailUseCase;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.port.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetStudentDetailService implements GetStudentDetailUseCase {

    private final StudentRepository studentRepository;
    private final UserDisplayNamePort userDisplayNamePort;

    public GetStudentDetailService(StudentRepository studentRepository,
                                   UserDisplayNamePort userDisplayNamePort) {
        this.studentRepository = studentRepository;
        this.userDisplayNamePort = userDisplayNamePort;
    }

    @Override
    public StudentDetail execute(UUID tenantId, UUID studentId) {
        Student student = studentRepository.findById(tenantId, studentId)
                .orElseThrow(() -> new StudentNotFoundException(
                        "Student with id '%s' not found".formatted(studentId)));

        String createdByName = resolveName(student.getCreatedBy());
        String updatedByName = student.getUpdatedBy() != null ? resolveName(student.getUpdatedBy()) : null;
        String deactivatedByName = student.getDeactivatedBy() != null ? resolveName(student.getDeactivatedBy()) : null;

        return StudentDetail.fromDomain(student, createdByName, updatedByName, deactivatedByName);
    }

    private String resolveName(UUID userId) {
        return userDisplayNamePort.findDisplayName(userId).orElse(userId.toString());
    }
}
