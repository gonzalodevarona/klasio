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
    void membershipLowHoursHasCorrectTemplateRefAndRequiredKeys() {
        assertThat(EmailType.MEMBERSHIP_LOW_HOURS.templateRef()).isEqualTo("membership-low-hours");
        assertThat(EmailType.MEMBERSHIP_LOW_HOURS.requiredKeys())
                .containsExactlyInAnyOrder("studentName", "remainingHours");
    }

    @Test
    void allTenTypesAreDefined() {
        assertThat(EmailType.values()).hasSize(10);
    }
}
