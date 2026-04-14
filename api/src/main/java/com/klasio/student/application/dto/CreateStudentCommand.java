package com.klasio.student.application.dto;

import com.klasio.student.domain.model.BloodType;
import com.klasio.shared.domain.model.IdentityDocumentType;

import java.time.LocalDate;
import java.util.UUID;

public record CreateStudentCommand(
        UUID tenantId,
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
        UUID createdBy
) {}
