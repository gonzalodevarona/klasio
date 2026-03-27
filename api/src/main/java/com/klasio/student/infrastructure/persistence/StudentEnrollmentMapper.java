package com.klasio.student.infrastructure.persistence;

import com.klasio.student.domain.model.Level;
import com.klasio.student.domain.model.StudentEnrollment;
import com.klasio.student.domain.model.StudentEnrollmentId;
import org.springframework.stereotype.Component;

@Component
public class StudentEnrollmentMapper {

    public StudentEnrollment toDomain(StudentEnrollmentJpaEntity entity) {
        return StudentEnrollment.reconstitute(
                StudentEnrollmentId.of(entity.getId()),
                entity.getTenantId(),
                entity.getStudentId(),
                entity.getProgramId(),
                Level.valueOf(entity.getLevel()),
                entity.getEnrollmentDate(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }

    public StudentEnrollmentJpaEntity toEntity(StudentEnrollment enrollment) {
        StudentEnrollmentJpaEntity entity = new StudentEnrollmentJpaEntity();
        entity.setId(enrollment.getId().value());
        entity.setTenantId(enrollment.getTenantId());
        entity.setStudentId(enrollment.getStudentId());
        entity.setProgramId(enrollment.getProgramId());
        entity.setLevel(enrollment.getLevel().name());
        entity.setEnrollmentDate(enrollment.getEnrollmentDate());
        entity.setStatus(enrollment.getStatus());
        entity.setCreatedAt(enrollment.getCreatedAt());
        entity.setCreatedBy(enrollment.getCreatedBy());
        entity.setUpdatedAt(enrollment.getUpdatedAt());
        entity.setUpdatedBy(enrollment.getUpdatedBy());
        return entity;
    }
}
