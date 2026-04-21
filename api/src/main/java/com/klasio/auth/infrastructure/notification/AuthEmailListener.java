package com.klasio.auth.infrastructure.notification;

import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.auth.domain.event.PasswordResetRequestedEvent;
import com.klasio.auth.domain.event.StudentRegisteredEvent;
import com.klasio.auth.domain.event.VerificationEmailResendRequested;
import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailService;
import com.klasio.email.application.EmailType;
import com.klasio.shared.infrastructure.web.FrontendUrlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Map;

@Slf4j
@Component
public class AuthEmailListener {

    private final EmailService emailService;
    private final FrontendUrlBuilder urlBuilder;
    private final TenantResolverPort tenantResolverPort;

    public AuthEmailListener(EmailService emailService,
                             FrontendUrlBuilder urlBuilder,
                             TenantResolverPort tenantResolverPort) {
        this.emailService = emailService;
        this.urlBuilder = urlBuilder;
        this.tenantResolverPort = tenantResolverPort;
    }

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStudentRegistered(StudentRegisteredEvent e) {
        String tenantSlug = tenantResolverPort.resolveSlugByTenantId(e.tenantId())
                .orElse("app");
        String verificationUrl = urlBuilder.build(tenantSlug,
                "/verify-email?token=" + e.rawToken());
        emailService.send(
                EmailType.STUDENT_VERIFICATION,
                new EmailRecipient(e.email(), e.displayName()),
                e.tenantId(),
                Map.of("studentName", e.displayName(),
                       "verificationUrl", verificationUrl,
                       "expiresAt", e.expiresAt().toString()));
    }

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent e) {
        String tenantSlug = tenantResolverPort.resolveSlugByTenantId(e.tenantId())
                .orElse("app");
        String resetUrl = urlBuilder.build(tenantSlug,
                "/reset-password?token=" + e.rawToken());
        emailService.send(
                EmailType.PASSWORD_RECOVERY,
                new EmailRecipient(e.email(), e.email()),
                e.tenantId(),
                Map.of("resetUrl", resetUrl,
                       "expiresAt", e.expiresAt().toString()));
    }

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationResendRequested(VerificationEmailResendRequested e) {
        String verificationUrl = urlBuilder.build(e.tenantSlug(),
                "/verify-email?token=" + e.rawToken());
        emailService.send(
                EmailType.STUDENT_VERIFICATION,
                new EmailRecipient(e.email(), e.email()),
                e.tenantId(),
                Map.of("studentName", e.email(),
                       "verificationUrl", verificationUrl,
                       "expiresAt", e.expiresAt().toString()));
    }
}
