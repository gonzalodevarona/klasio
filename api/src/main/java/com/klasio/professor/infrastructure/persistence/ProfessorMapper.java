package com.klasio.professor.infrastructure.persistence;

import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.model.ProfessorId;
import com.klasio.professor.domain.model.ProfessorStatus;
import org.springframework.stereotype.Component;

@Component
public class ProfessorMapper {

    public Professor toDomain(ProfessorJpaEntity entity) {
        return Professor.reconstitute(
                ProfessorId.of(entity.getId()),
                entity.getTenantId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                ProfessorStatus.valueOf(entity.getStatus()),
                entity.getInvitationToken(),
                entity.getInvitationExpiresAt(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy(),
                entity.getIdentityDocumentType(),
                entity.getIdentityNumber()
        );
    }

    public ProfessorJpaEntity toEntity(Professor professor) {
        ProfessorJpaEntity entity = new ProfessorJpaEntity();
        entity.setId(professor.getId().value());
        entity.setTenantId(professor.getTenantId());
        entity.setFirstName(professor.getFirstName());
        entity.setLastName(professor.getLastName());
        entity.setEmail(professor.getEmail());
        entity.setPhoneNumber(professor.getPhoneNumber());
        entity.setStatus(professor.getStatus().name());
        entity.setInvitationToken(professor.getInvitationToken());
        entity.setInvitationExpiresAt(professor.getInvitationExpiresAt());
        entity.setCreatedAt(professor.getCreatedAt());
        entity.setCreatedBy(professor.getCreatedBy());
        entity.setUpdatedAt(professor.getUpdatedAt());
        entity.setUpdatedBy(professor.getUpdatedBy());
        entity.setIdentityDocumentType(professor.getIdentityDocumentType());
        entity.setIdentityNumber(professor.getIdentityNumber());
        return entity;
    }
}
