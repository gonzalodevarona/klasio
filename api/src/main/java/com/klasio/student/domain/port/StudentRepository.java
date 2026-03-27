package com.klasio.student.domain.port;

import com.klasio.student.domain.model.Student;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.UUID;

public interface StudentRepository {

    void save(Student student);

    Optional<Student> findById(UUID tenantId, UUID studentId);

    boolean existsByEmailInTenant(UUID tenantId, String email);

    boolean existsByEmailInTenantExcluding(UUID tenantId, String email, UUID excludeId);

    Page<Student> findAll(UUID tenantId, int page, int size, String status, String search);
}
