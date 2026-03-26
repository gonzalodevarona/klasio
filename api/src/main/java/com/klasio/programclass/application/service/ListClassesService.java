package com.klasio.programclass.application.service;

import com.klasio.programclass.application.dto.ClassSummary;
import com.klasio.programclass.application.port.input.ListClassesUseCase;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassStatus;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ListClassesService implements ListClassesUseCase {

    private final ProgramClassRepository programClassRepository;

    public ListClassesService(ProgramClassRepository programClassRepository) {
        this.programClassRepository = programClassRepository;
    }

    @Override
    public Page<ClassSummary> execute(UUID tenantId, UUID programId, ClassLevel level,
                                      ClassStatus status, Pageable pageable) {
        return programClassRepository.findByProgramId(tenantId, programId, pageable, level, status)
                .map(ClassSummary::fromDomain);
    }
}
