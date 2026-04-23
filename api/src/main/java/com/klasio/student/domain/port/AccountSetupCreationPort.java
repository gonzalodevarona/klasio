package com.klasio.student.domain.port;

import com.klasio.shared.domain.model.IdentityDocumentType;

import java.util.UUID;

public interface AccountSetupCreationPort {

    /**
     * Creates a user account in INVITED state (passwordHash=null),
     * and dispatches an AccountSetupInitiated event so the user receives a
     * 15-minute link to set their password.
     * Must be called within an existing transaction (MANDATORY propagation).
     *
     * @return the newly created user's UUID
     */
    UUID createAndDispatchSetup(UUID tenantId, String email, String firstName, String lastName,
            IdentityDocumentType docType, String identityNumber, String phone);
}
