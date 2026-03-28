package com.klasio.student.application.service;

import com.klasio.student.application.dto.StudentSummary;
import com.klasio.student.application.port.input.ListStudentsUseCase;
import com.klasio.student.domain.port.ActiveMembershipPort;
import com.klasio.student.domain.port.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ListStudentsService implements ListStudentsUseCase {

    private final StudentRepository studentRepository;
    private final ActiveMembershipPort activeMembershipPort;

    public ListStudentsService(StudentRepository studentRepository,
                               ActiveMembershipPort activeMembershipPort) {
        this.studentRepository = studentRepository;
        this.activeMembershipPort = activeMembershipPort;
    }

    @Override
    public Page<StudentSummary> execute(UUID tenantId, int page, int size, String status, String search) {
        return studentRepository.findAll(tenantId, page, size, status, search)
                .map(student -> StudentSummary.fromDomain(
                        student,
                        activeMembershipPort.hasActiveMembership(tenantId, student.getId().value())
                ));
    }
}
