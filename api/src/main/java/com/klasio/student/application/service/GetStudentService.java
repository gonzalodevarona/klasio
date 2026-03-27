package com.klasio.student.application.service;

import com.klasio.shared.infrastructure.exception.StudentNotFoundException;
import com.klasio.student.application.port.input.GetStudentUseCase;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.port.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetStudentService implements GetStudentUseCase {

    private final StudentRepository studentRepository;

    public GetStudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public Student execute(UUID tenantId, UUID studentId) {
        return studentRepository.findById(tenantId, studentId)
                .orElseThrow(() -> new StudentNotFoundException(
                        "Student with id '%s' not found".formatted(studentId)));
    }
}
