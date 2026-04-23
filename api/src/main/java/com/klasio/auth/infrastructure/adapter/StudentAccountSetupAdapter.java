package com.klasio.auth.infrastructure.adapter;

import com.klasio.auth.domain.model.Role;
import com.klasio.shared.domain.model.IdentityDocumentType;
import com.klasio.student.domain.port.AccountSetupCreationPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implements {@link AccountSetupCreationPort} for the student module.
 * Lives in the auth module because it orchestrates User creation and
 * AccountSetupToken generation — both auth concerns.
 * Must run inside an existing transaction (MANDATORY) to ensure atomicity
 * with the student record created by the caller.
 */
@Component
public class StudentAccountSetupAdapter implements AccountSetupCreationPort {

    private final AccountSetupCreationSupport support;

    public StudentAccountSetupAdapter(AccountSetupCreationSupport support) {
        this.support = support;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID createAndDispatchSetup(UUID tenantId, String email, String firstName, String lastName,
            IdentityDocumentType docType, String identityNumber, String phone) {
        return support.createAndDispatch(tenantId, email, firstName, lastName,
                docType, identityNumber, phone, Role.STUDENT, "student");
    }
}
