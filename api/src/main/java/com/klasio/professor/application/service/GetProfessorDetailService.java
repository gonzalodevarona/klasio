package com.klasio.professor.application.service;

import com.klasio.professor.application.dto.ProfessorDetail;
import com.klasio.professor.application.port.input.GetProfessorDetailUseCase;
import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.shared.domain.port.UserDisplayNamePort;
import com.klasio.shared.infrastructure.exception.ProfessorNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetProfessorDetailService implements GetProfessorDetailUseCase {

    private final ProfessorRepository professorRepository;
    private final UserDisplayNamePort userDisplayNamePort;

    public GetProfessorDetailService(ProfessorRepository professorRepository,
                                     UserDisplayNamePort userDisplayNamePort) {
        this.professorRepository = professorRepository;
        this.userDisplayNamePort = userDisplayNamePort;
    }

    @Override
    public ProfessorDetail execute(UUID tenantId, UUID professorId) {
        Professor professor = professorRepository.findById(tenantId, professorId)
                .orElseThrow(() -> new ProfessorNotFoundException("Professor not found"));

        String createdByName = resolveName(professor.getCreatedBy());
        String updatedByName = professor.getUpdatedBy() != null ? resolveName(professor.getUpdatedBy()) : null;

        return ProfessorDetail.fromDomain(professor, createdByName, updatedByName);
    }

    private String resolveName(UUID userId) {
        return userDisplayNamePort.findDisplayName(userId).orElse(userId.toString());
    }
}
