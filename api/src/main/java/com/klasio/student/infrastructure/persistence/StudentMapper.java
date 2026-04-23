package com.klasio.student.infrastructure.persistence;

import com.klasio.student.domain.model.BloodType;
import com.klasio.shared.domain.model.IdentityDocumentType;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.model.StudentId;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

    public Student toDomain(StudentJpaEntity entity) {
        return Student.reconstitute(
                StudentId.of(entity.getId()),
                entity.getTenantId(),
                entity.getUserId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getDateOfBirth(),
                entity.getEps(),
                entity.getIdentityNumber(),
                IdentityDocumentType.valueOf(entity.getIdentityDocumentType()),
                entity.getBloodType() != null ? BloodType.fromLabel(entity.getBloodType()) : null,
                entity.getPhone(),
                entity.getTutorFirstName(),
                entity.getTutorLastName(),
                entity.getTutorRelationship(),
                entity.getTutorPhone(),
                entity.getTutorEmail(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy(),
                entity.getDeactivatedAt(),
                entity.getDeactivatedBy()
        );
    }

    public StudentJpaEntity toEntity(Student student) {
        StudentJpaEntity entity = new StudentJpaEntity();
        entity.setId(student.getId().value());
        entity.setTenantId(student.getTenantId());
        entity.setUserId(student.getUserId());
        entity.setFirstName(student.getFirstName());
        entity.setLastName(student.getLastName());
        entity.setEmail(student.getEmail());
        entity.setDateOfBirth(student.getDateOfBirth());
        entity.setEps(student.getEps());
        entity.setIdentityNumber(student.getIdentityNumber());
        entity.setIdentityDocumentType(student.getIdentityDocumentType().name());
        entity.setBloodType(student.getBloodType() != null ? student.getBloodType().label() : null);
        entity.setPhone(student.getPhone());
        entity.setTutorFirstName(student.getTutorFirstName());
        entity.setTutorLastName(student.getTutorLastName());
        entity.setTutorRelationship(student.getTutorRelationship());
        entity.setTutorPhone(student.getTutorPhone());
        entity.setTutorEmail(student.getTutorEmail());
        entity.setStatus(student.getStatus());
        entity.setCreatedAt(student.getCreatedAt());
        entity.setCreatedBy(student.getCreatedBy());
        entity.setUpdatedAt(student.getUpdatedAt());
        entity.setUpdatedBy(student.getUpdatedBy());
        entity.setDeactivatedAt(student.getDeactivatedAt());
        entity.setDeactivatedBy(student.getDeactivatedBy());
        return entity;
    }
}
