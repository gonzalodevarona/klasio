package com.klasio.email.application;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EmailTypeTest {

    @Test
    void inRepoTypesHaveTemplateRefWithoutPrefix() {
        assertThat(EmailType.STUDENT_VERIFICATION.source()).isEqualTo(EmailType.Source.IN_REPO);
        assertThat(EmailType.STUDENT_VERIFICATION.templateRef()).isEqualTo("student-verification");
        assertThat(EmailType.STUDENT_VERIFICATION.requiredKeys())
                .containsExactlyInAnyOrder("verificationUrl", "expiresAt", "studentName");
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
