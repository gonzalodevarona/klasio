package com.klasio.programclass.application.service;

import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.programclass.application.dto.ClassDetail;
import com.klasio.programclass.application.port.input.GetClassDetailUseCase;
import com.klasio.programclass.domain.port.ProgramClassRepository;
import com.klasio.shared.domain.port.UserDisplayNamePort;
import com.klasio.shared.infrastructure.exception.ClassNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetClassDetailService implements GetClassDetailUseCase {

    private final ProgramClassRepository programClassRepository;
    private final ProfessorRepository professorRepository;
    private final UserDisplayNamePort userDisplayNamePort;

    public GetClassDetailService(ProgramClassRepository programClassRepository,
                                 ProfessorRepository professorRepository,
                                 UserDisplayNamePort userDisplayNamePort) {
        this.programClassRepository = programClassRepository;
        this.professorRepository = professorRepository;
        this.userDisplayNamePort = userDisplayNamePort;
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

        String createdByName = resolveName(pc.getCreatedBy());
        String updatedByName = pc.getUpdatedBy() != null ? resolveName(pc.getUpdatedBy()) : null;

        var base = ClassDetail.fromDomain(pc, createdByName, updatedByName);
        return new ClassDetail(
                base.id(), base.tenantId(), base.programId(),
                base.name(), base.level(), base.type(),
                base.professorId(), professorName,
                base.maxStudents(), base.status(), base.scheduleEntries(),
                base.createdAt(), base.createdBy(), base.updatedAt(), base.updatedBy()
        );
    }

    private String resolveName(UUID userId) {
        return userDisplayNamePort.findDisplayName(userId).orElse(userId.toString());
    }
}
