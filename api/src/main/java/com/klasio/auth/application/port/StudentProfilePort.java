package com.klasio.auth.application.port;

import java.time.LocalDate;
import java.util.UUID;

public interface StudentProfilePort {

    UUID createStudentProfile(UUID tenantId, String firstName, String lastName, String email,
                              LocalDate dateOfBirth, String documentType, String documentNumber,
                              String eps, String tutorFullName, String tutorRelationship,
                              String tutorContact, UUID userId);

    boolean existsByIdentityNumberInTenant(UUID tenantId, String identityNumber);
}
