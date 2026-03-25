package com.klasio.professor.application.service;

import com.klasio.professor.application.dto.ProfessorSummary;
import com.klasio.professor.application.port.input.ListProfessorsUseCase;
import com.klasio.professor.domain.model.ProfessorStatus;
import com.klasio.professor.domain.port.ProfessorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListProfessorsService implements ListProfessorsUseCase {

    private final ProfessorRepository professorRepository;

    public ListProfessorsService(ProfessorRepository professorRepository) {
        this.professorRepository = professorRepository;
    }

    @Override
    public Page<ProfessorSummary> execute(UUID tenantId, Pageable pageable, String status) {
        ProfessorStatus professorStatus = (status != null && !status.isBlank())
                ? ProfessorStatus.valueOf(status)
                : null;

        return professorRepository.findAllByTenant(tenantId, pageable, professorStatus)
                .map(ProfessorSummary::fromDomain);
    }
}
