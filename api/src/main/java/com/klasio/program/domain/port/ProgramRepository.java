package com.klasio.program.domain.port;

import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.model.ProgramStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ProgramRepository {

    void save(Program program);

    Optional<Program> findById(UUID tenantId, UUID programId);

    boolean existsByNameInTenant(UUID tenantId, String name);

    boolean existsByNameInTenantExcluding(UUID tenantId, String name, UUID excludeProgramId);

    Page<Program> findAllByTenant(UUID tenantId, Pageable pageable, ProgramStatus status);
}
