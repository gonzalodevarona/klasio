package com.klasio.auth.infrastructure.notification;

import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.auth.domain.event.PasswordResetRequestedEvent;
import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailService;
import com.klasio.email.application.EmailType;
import com.klasio.shared.infrastructure.web.FrontendUrlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class PasswordResetEmailListener {

    private final EmailService emailService;
    private final FrontendUrlBuilder urlBuilder;
    private final TenantResolverPort tenantResolverPort;

    public PasswordResetEmailListener(EmailService emailService,
                                      FrontendUrlBuilder urlBuilder,
                                      TenantResolverPort tenantResolverPort) {
        this.emailService = emailService;
        this.urlBuilder = urlBuilder;
        this.tenantResolverPort = tenantResolverPort;
    }

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        if (event.email() == null || event.email().isBlank()) {
            log.warn("[EMAIL] Skipping PASSWORD_RECOVERY: blank email for userId={}", event.userId());
            return;
        }
        String tenantSlug = tenantResolverPort.resolveSlugByTenantId(event.tenantId())
                .orElse("app");
        String resetUrl = urlBuilder.build(tenantSlug, "/reset-password?token=" + event.rawToken());

        Map<String, Object> params = new HashMap<>();
        params.put("resetUrl", resetUrl);
        params.put("expiresAt", event.expiresAt().toString());
        // tenantName is injected automatically by EmailDispatcherService via TenantContext

        emailService.send(
                EmailType.PASSWORD_RECOVERY,
                new EmailRecipient(event.email(), event.recipientName()),
                event.tenantId(),
                params);
    }
}
