package com.klasio.email.application;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EmailTypeTest {

    @Test
    void inRepoTypesHaveTemplateRefWithoutPrefix() {
        assertThat(EmailType.ACCOUNT_SETUP.source()).isEqualTo(EmailType.Source.IN_REPO);
        assertThat(EmailType.ACCOUNT_SETUP.templateRef()).isEqualTo("account-setup");
        assertThat(EmailType.ACCOUNT_SETUP.requiredKeys())
                .containsExactlyInAnyOrder("recipientName", "role", "tenantName", "setupUrl", "expiresAt");
    }

    @Test
    void brevoHostedTypesHaveShortTemplateRef() {
        assertThat(EmailType.MEMBERSHIP_ACTIVATED.source()).isEqualTo(EmailType.Source.BREVO_HOSTED);
        assertThat(EmailType.MEMBERSHIP_ACTIVATED.templateRef()).isEqualTo("membership-activated");
    }

    @Test
    void allNineTypesAreDefined() {
        assertThat(EmailType.values()).hasSize(9);
    }
}
