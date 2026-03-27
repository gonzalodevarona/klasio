package com.klasio.student.domain.port;

import com.klasio.student.domain.model.StudentEnrollment;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.UUID;

public interface StudentEnrollmentRepository {

    void save(StudentEnrollment enrollment);

    Optional<StudentEnrollment> findById(UUID tenantId, UUID enrollmentId);

    boolean existsByStudentIdAndProgramIdAndLevelActive(UUID studentId, UUID programId, String level);

    Optional<StudentEnrollment> findActiveByStudentIdAndProgramIdAndLevel(UUID tenantId, UUID studentId, UUID programId, String level);

    Page<StudentEnrollment> findByProgramId(UUID tenantId, UUID programId, int page, int size, String level, String status);

    Page<StudentEnrollment> findByStudentId(UUID tenantId, UUID studentId, int page, int size, String status);
}
