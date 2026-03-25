package com.klasio.professor.application.service;

import com.klasio.professor.application.dto.ProfessorDetail;
import com.klasio.professor.application.port.input.GetProfessorDetailUseCase;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.shared.infrastructure.exception.ProfessorNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetProfessorDetailService implements GetProfessorDetailUseCase {

    private final ProfessorRepository professorRepository;

    public GetProfessorDetailService(ProfessorRepository professorRepository) {
        this.professorRepository = professorRepository;
    }

    @Override
    public ProfessorDetail execute(UUID tenantId, UUID professorId) {
        return professorRepository.findById(tenantId, professorId)
                .map(ProfessorDetail::fromDomain)
                .orElseThrow(() -> new ProfessorNotFoundException("Professor not found"));
    }
}
