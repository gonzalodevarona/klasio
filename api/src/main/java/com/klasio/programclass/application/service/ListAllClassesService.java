package com.klasio.programclass.application.service;

import com.klasio.programclass.application.dto.ClassSummary;
import com.klasio.programclass.application.port.input.ListAllClassesUseCase;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassStatus;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ListAllClassesService implements ListAllClassesUseCase {

    private final ProgramClassRepository programClassRepository;

    public ListAllClassesService(ProgramClassRepository programClassRepository) {
        this.programClassRepository = programClassRepository;
    }

    @Override
    public Page<ClassSummary> execute(UUID tenantId, ClassLevel level, ClassStatus status,
                                      String programName, Pageable pageable) {
        return programClassRepository.findByTenantIdWithProgramName(
                tenantId, pageable, level, status,
                (programName != null && !programName.isBlank()) ? programName : null);
    }
}
