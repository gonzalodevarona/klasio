package com.klasio.student.application.service;

import com.klasio.student.application.dto.StudentSummary;
import com.klasio.student.application.port.input.ListStudentsUseCase;
import com.klasio.student.domain.port.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ListStudentsService implements ListStudentsUseCase {

    private final StudentRepository studentRepository;

    public ListStudentsService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public Page<StudentSummary> execute(UUID tenantId, int page, int size, String status, String search) {
        return studentRepository.findAll(tenantId, page, size, status, search)
                .map(StudentSummary::fromDomain);
    }
}
