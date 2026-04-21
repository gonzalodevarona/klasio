package com.klasio.auth.infrastructure.mail;

import com.klasio.auth.application.port.AuthEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stub implementation — will be replaced by the RF-32 EmailService adapter in Task 12.
 * Auth emails (verification, password reset) will be dispatched via EmailService.send()
 * using EmailType.STUDENT_VERIFICATION and EmailType.PASSWORD_RECOVERY.
 */
@Component
public class PostmarkAuthEmailSender implements AuthEmailSender {

    private static final Logger log = LoggerFactory.getLogger(PostmarkAuthEmailSender.class);

    @Override
    public void sendVerificationEmail(String toEmail, String rawToken, String tenantSlug) {
        // TODO (RF-32 Task 12): delegate to EmailService.send(STUDENT_VERIFICATION, ...)
        log.info("[RF-32 stub] sendVerificationEmail to={} tenantSlug={}", toEmail, tenantSlug);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        // TODO (RF-32 Task 12): delegate to EmailService.send(PASSWORD_RECOVERY, ...)
        log.info("[RF-32 stub] sendPasswordResetEmail to={}", toEmail);
    }
}
