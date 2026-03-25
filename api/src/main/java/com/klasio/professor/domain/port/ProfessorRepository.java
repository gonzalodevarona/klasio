package com.klasio.professor.domain.port;

import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.model.ProfessorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ProfessorRepository {

    void save(Professor professor);

    Optional<Professor> findById(UUID tenantId, UUID professorId);

    boolean existsByEmailInTenant(UUID tenantId, String email);

    boolean existsByEmailInTenantExcluding(UUID tenantId, String email, UUID excludeId);

    Page<Professor> findAllByTenant(UUID tenantId, Pageable pageable, ProfessorStatus status);
}
