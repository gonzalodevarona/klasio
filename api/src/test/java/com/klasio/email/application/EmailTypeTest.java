package com.klasio.email.application;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EmailTypeTest {

    @Test
    void accountSetupHasCorrectTemplateRefAndRequiredKeys() {
        assertThat(EmailType.ACCOUNT_SETUP.templateRef()).isEqualTo("account-setup");
        assertThat(EmailType.ACCOUNT_SETUP.requiredKeys())
                .containsExactlyInAnyOrder("recipientName", "role", "setupUrl", "expiresAt");
    }

    @Test
    void membershipActivatedIsInRepoWithCorrectRequiredKeys() {
        assertThat(EmailType.MEMBERSHIP_ACTIVATED.templateRef()).isEqualTo("membership-activated");
        assertThat(EmailType.MEMBERSHIP_ACTIVATED.requiredKeys())
                .containsExactlyInAnyOrder("studentName", "programName", "planName", "totalHours", "expiresAt");
    }

    @Test
    void classSessionChangeHasCorrectRequiredKeys() {
        assertThat(EmailType.CLASS_SESSION_CHANGE.templateRef()).isEqualTo("class-session-change");
        assertThat(EmailType.CLASS_SESSION_CHANGE.requiredKeys())
                .containsExactlyInAnyOrder("studentName", "className", "startsAt", "changeKind", "reason");
    }

    @Test
    void allNineTypesAreDefined() {
        assertThat(EmailType.values()).hasSize(9);
    }
}
