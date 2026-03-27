package com.klasio.student.application.dto;

import com.klasio.student.domain.model.BloodType;
import com.klasio.student.domain.model.IdentityDocumentType;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateStudentCommand(
        UUID tenantId,
        UUID studentId,
        String firstName,
        String lastName,
        String email,
        LocalDate dateOfBirth,
        String eps,
        String identityNumber,
        IdentityDocumentType identityDocumentType,
        BloodType bloodType,
        String phone,
        String tutorFirstName,
        String tutorLastName,
        String tutorRelationship,
        String tutorPhone,
        String tutorEmail,
        UUID updatedBy
) {}
