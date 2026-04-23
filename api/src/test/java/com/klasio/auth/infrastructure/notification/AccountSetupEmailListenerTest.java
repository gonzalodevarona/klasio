package com.klasio.auth.infrastructure.notification;

import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.auth.domain.event.AccountSetupInitiated;
import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailService;
import com.klasio.email.application.EmailType;
import com.klasio.shared.infrastructure.web.FrontendUrlBuilder;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.Mockito.*;

class AccountSetupEmailListenerTest {

    private final EmailService emailService = mock(EmailService.class);
    private final FrontendUrlBuilder urlBuilder = mock(FrontendUrlBuilder.class);
    private final TenantResolverPort tenantResolverPort = mock(TenantResolverPort.class);

    private final AccountSetupEmailListener listener =
            new AccountSetupEmailListener(emailService, urlBuilder, tenantResolverPort);

    @Test
    void onAccountSetupInitiated_sendsEmail() {
        UUID tenantId = UUID.randomUUID();
        when(tenantResolverPort.resolveSlugByTenantId(tenantId)).thenReturn(Optional.of("test-league"));
        when(urlBuilder.build("test-league", "/setup-account?token=tok123"))
                .thenReturn("https://test-league.klasio.com/setup-account?token=tok123");

        listener.onAccountSetupInitiated(new AccountSetupInitiated(
                UUID.randomUUID(), tenantId, "student@test.com", "Carlos López",
                "STUDENT", "tok123", Instant.now().plusSeconds(900), Instant.now()));

        verify(emailService).send(
                eq(EmailType.ACCOUNT_SETUP),
                eq(new EmailRecipient("student@test.com", "Carlos López")),
                eq(tenantId),
                argThat(params -> params.containsKey("setupUrl") && params.containsKey("recipientName")));
    }

    @Test
    void onAccountSetupInitiated_withBlankEmail_skipsEmail() {
        listener.onAccountSetupInitiated(new AccountSetupInitiated(
                UUID.randomUUID(), UUID.randomUUID(), "", "Carlos",
                "STUDENT", "tok", Instant.now().plusSeconds(900), Instant.now()));
        verifyNoInteractions(emailService);
    }
}
