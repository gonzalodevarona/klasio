package com.klasio.programclass.domain.port;

import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassStatus;
import com.klasio.programclass.domain.model.ProgramClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ProgramClassRepository {

    void save(ProgramClass programClass);

    Optional<ProgramClass> findById(UUID tenantId, UUID classId);

    Page<ProgramClass> findByProgramId(UUID tenantId, UUID programId, Pageable pageable,
                                       ClassLevel level, ClassStatus status);

    Page<com.klasio.programclass.application.dto.ClassSummary> findByTenantIdWithProgramName(
            UUID tenantId, Pageable pageable,
            ClassLevel level, ClassStatus status, String programName);

    boolean existsByNameInProgram(UUID programId, String name);

    boolean existsByNameInProgramExcluding(UUID programId, String name, UUID excludeClassId);
}
