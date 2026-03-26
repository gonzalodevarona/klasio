package com.klasio.programclass.application.service;

import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.programclass.application.dto.ClassDetail;
import com.klasio.programclass.application.port.input.GetClassDetailUseCase;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetClassDetailService implements GetClassDetailUseCase {

    private final ProgramClassRepository programClassRepository;
    private final ProfessorRepository professorRepository;

    public GetClassDetailService(ProgramClassRepository programClassRepository,
                                 ProfessorRepository professorRepository) {
        this.programClassRepository = programClassRepository;
        this.professorRepository = professorRepository;
    }

    @Override
    public ClassDetail execute(UUID tenantId, UUID classId) {
        var pc = programClassRepository.findById(tenantId, classId)
                .orElseThrow(() -> new ClassNotFoundException(
                        "Class with id '%s' not found".formatted(classId)));

        String professorName = null;
        if (pc.getProfessorId() != null) {
            professorName = professorRepository.findById(tenantId, pc.getProfessorId())
                    .map(p -> p.getFirstName() + " " + p.getLastName())
                    .orElse(null);
        }

        var base = ClassDetail.fromDomain(pc);
        return new ClassDetail(
                base.id(), base.tenantId(), base.programId(),
                base.name(), base.level(), base.type(),
                base.professorId(), professorName,
                base.maxStudents(), base.status(), base.scheduleEntries(),
                base.createdAt(), base.createdBy(), base.updatedAt(), base.updatedBy()
        );
    }
}
