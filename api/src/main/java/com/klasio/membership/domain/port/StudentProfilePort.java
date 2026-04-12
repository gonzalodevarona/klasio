package com.klasio.membership.domain.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Port (outbound) for resolving a student's identity profile from the student module.
 * Returns the full name and identity document info needed for the payment proof queue.
 * Zero Spring imports — implemented by infrastructure adapter.
 */
public interface StudentProfilePort {

    record StudentProfile(
            String fullName,
            String identityDocumentType,
            String identityNumber
    ) {}

    Optional<StudentProfile> findProfile(UUID studentId, UUID tenantId);
}
