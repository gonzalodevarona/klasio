package com.klasio.auth.infrastructure.notification;

import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.auth.domain.event.PasswordResetRequestedEvent;
import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailService;
import com.klasio.email.application.EmailType;
import com.klasio.shared.infrastructure.web.FrontendUrlBuilder;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.Mockito.*;

class PasswordResetEmailListenerTest {

    private final EmailService emailService = mock(EmailService.class);
    private final FrontendUrlBuilder urlBuilder = mock(FrontendUrlBuilder.class);
    private final TenantResolverPort tenantResolverPort = mock(TenantResolverPort.class);

    private final PasswordResetEmailListener listener =
            new PasswordResetEmailListener(emailService, urlBuilder, tenantResolverPort);

    @Test
    void onPasswordResetRequested_sendsEmail() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String email = "user@test.com";
        String recipientName = "John Doe";
        String rawToken = "token123";
        Instant expiresAt = Instant.now().plusSeconds(1800);

        when(tenantResolverPort.resolveSlugByTenantId(tenantId)).thenReturn(Optional.of("test-league"));
        when(urlBuilder.build("test-league", "/reset-password?token=" + rawToken))
                .thenReturn("https://test-league.klasio.com/reset-password?token=" + rawToken);

        listener.onPasswordResetRequested(new PasswordResetRequestedEvent(
                userId, tenantId, email, recipientName, rawToken, expiresAt, Instant.now()));

        verify(emailService).send(
                eq(EmailType.PASSWORD_RECOVERY),
                eq(new EmailRecipient(email, recipientName)),
                eq(tenantId),
                argThat(params ->
                    params.containsKey("resetUrl") &&
                    params.get("resetUrl").toString().contains(rawToken) &&
                    params.containsKey("expiresAt") &&
                    params.get("expiresAt") instanceof Instant));
    }

    @Test
    void onPasswordResetRequested_withBlankEmail_skipsEmail() {
        listener.onPasswordResetRequested(new PasswordResetRequestedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "", "John Doe",
                "token123", Instant.now().plusSeconds(1800), Instant.now()));
        verifyNoInteractions(emailService);
    }
}
