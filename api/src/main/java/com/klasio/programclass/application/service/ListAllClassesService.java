package com.klasio.programclass.application.service;

import com.klasio.programclass.application.dto.ClassSummary;
import com.klasio.programclass.application.port.input.ListAllClassesUseCase;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassStatus;
import com.klasio.programclass.domain.port.ProfessorIdLookupPort;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ListAllClassesService implements ListAllClassesUseCase {

    private final ProgramClassRepository programClassRepository;
    private final ProfessorIdLookupPort professorIdLookupPort;

    public ListAllClassesService(ProgramClassRepository programClassRepository,
                                 ProfessorIdLookupPort professorIdLookupPort) {
        this.programClassRepository = programClassRepository;
        this.professorIdLookupPort = professorIdLookupPort;
    }

    @Override
    public Page<ClassSummary> execute(UUID tenantId, ClassLevel level, ClassStatus status,
                                      String programName, Pageable pageable) {
        return programClassRepository.findByTenantIdWithProgramName(
                tenantId, pageable, level, status,
                (programName != null && !programName.isBlank()) ? programName : null);
    }

    @Override
    public Page<ClassSummary> execute(UUID tenantId, ClassLevel level, ClassStatus status,
                                      Pageable pageable, UUID professorId) {
        return programClassRepository.findByTenantIdAndProfessorId(tenantId, pageable, level, status, professorId);
    }

    @Override
    public Page<ClassSummary> execute(UUID tenantId, ClassLevel level, ClassStatus status,
                                      String programName, Pageable pageable, UUID userId, String role) {
        if ("PROFESSOR".equals(role)) {
            Optional<UUID> professorIdOpt = professorIdLookupPort.findProfessorIdByUserId(tenantId, userId);
            if (professorIdOpt.isEmpty()) {
                return Page.empty(pageable);
            }
            return programClassRepository.findByTenantIdAndProfessorId(tenantId, pageable, level, status, professorIdOpt.get());
        }
        return execute(tenantId, level, status, programName, pageable);
    }
}
