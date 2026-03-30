package com.klasio.auth.infrastructure.adapter;

import com.klasio.auth.application.port.StudentProfilePort;
import com.klasio.student.infrastructure.persistence.SpringDataStudentRepository;
import com.klasio.student.infrastructure.persistence.StudentJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class StudentProfileAdapter implements StudentProfilePort {

    private final SpringDataStudentRepository studentRepository;

    public StudentProfileAdapter(SpringDataStudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public UUID createStudentProfile(UUID tenantId, String firstName, String lastName, String email,
                                     LocalDate dateOfBirth, String documentType, String documentNumber,
                                     String eps, String tutorFullName, String tutorRelationship,
                                     String tutorContact, UUID userId) {
        StudentJpaEntity entity = new StudentJpaEntity();
        UUID studentId = UUID.randomUUID();
        entity.setId(studentId);
        entity.setTenantId(tenantId);
        entity.setFirstName(firstName);
        entity.setLastName(lastName);
        entity.setEmail(email.toLowerCase().trim());
        entity.setDateOfBirth(dateOfBirth);
        entity.setIdentityDocumentType(documentType);
        entity.setIdentityNumber(documentNumber);
        entity.setEps(eps);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(Instant.now());
        entity.setCreatedBy(userId);
        entity.setUserId(userId);
        entity.markAsNew();

        if (tutorFullName != null && !tutorFullName.isBlank()) {
            String[] parts = tutorFullName.trim().split("\\s+", 2);
            entity.setTutorFirstName(parts[0]);
            entity.setTutorLastName(parts.length > 1 ? parts[1] : "");
        }
        if (tutorRelationship != null && !tutorRelationship.isBlank()) {
            entity.setTutorRelationship(tutorRelationship.trim());
        }
        if (tutorContact != null && !tutorContact.isBlank()) {
            entity.setTutorPhone(tutorContact.trim());
        }

        studentRepository.save(entity);
        return studentId;
    }

    @Override
    public boolean existsByIdentityNumberInTenant(UUID tenantId, String identityNumber) {
        return studentRepository.existsByTenantIdAndIdentityNumber(tenantId, identityNumber);
    }
}
