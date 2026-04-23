package com.klasio.email.infrastructure.service;

import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailType;
import com.klasio.email.domain.model.*;
import com.klasio.email.domain.port.EmailTransport;
import com.klasio.email.domain.port.TemplateRenderer;
import com.klasio.email.domain.port.TenantContextPort;
import com.klasio.email.infrastructure.config.BrevoProperties;
import com.klasio.email.infrastructure.config.EmailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailDispatcherServiceTest {

    private final EmailTransport transport = mock(EmailTransport.class);
    private final TemplateRenderer renderer = mock(TemplateRenderer.class);
    private final TenantContextPort tenantContextPort = mock(TenantContextPort.class);

    private final EmailProperties props = new EmailProperties(
            "logging",
            new EmailProperties.FromProperties("noreply@klasio.app", " via Klasio"),
            new EmailProperties.RetryProperties(3, 1000, 5.0, 30000));

    private final BrevoProperties brevoProps = new BrevoProperties(
            "test-key", "https://api.brevo.com/v3",
            Map.of("membership-activated", 42L));

    private EmailDispatcherService service;

    private final UUID tenantId = UUID.randomUUID();
    private final TenantContext tenant = new TenantContext(tenantId, "test-league", "Test League");

    @BeforeEach
    void setUp() {
        when(tenantContextPort.findById(tenantId)).thenReturn(tenant);
        service = new EmailDispatcherService(transport, renderer, tenantContextPort, props, brevoProps);
    }

    @Test
    void inRepoType_rendersTemplateAndPassesToTransport() {
        when(renderer.render(eq("account-setup"), anyMap()))
                .thenReturn(new RenderedTemplate("Set up your account", "<html/>", "plain text"));

        service.send(EmailType.ACCOUNT_SETUP,
                new EmailRecipient("user@example.com", "Juan"),
                tenantId,
                Map.of("recipientName", "Juan", "role", "student", "tenantName", "Test League",
                       "setupUrl", "https://x.com", "expiresAt", "2026-05-01"));

        var captor = org.mockito.ArgumentCaptor.forClass(OutboundEmail.class);
        verify(transport).send(captor.capture());
        OutboundEmail sent = captor.getValue();
        assertThat(sent.type()).isEqualTo(EmailType.ACCOUNT_SETUP);
        assertThat(sent.subject()).isEqualTo("Set up your account");
        assertThat(sent.htmlBody()).isEqualTo("<html/>");
        assertThat(sent.brevoTemplateId()).isNull();
        assertThat(sent.from().email()).isEqualTo("noreply@klasio.app");
        assertThat(sent.from().displayName()).isEqualTo("Test League via Klasio");
    }

    @Test
    void brevoHostedType_withConfiguredTemplateId_passesTemplateIdToTransport() {
        service.send(EmailType.MEMBERSHIP_ACTIVATED,
                new EmailRecipient("s@example.com", "Ana"),
                tenantId,
                Map.of("studentName", "Ana", "programName", "Tennis",
                       "planName", "Monthly", "totalHours", 20, "expiresAt", "2026-05-31"));

        var captor = org.mockito.ArgumentCaptor.forClass(OutboundEmail.class);
        verify(transport).send(captor.capture());
        OutboundEmail sent = captor.getValue();
        assertThat(sent.brevoTemplateId()).isEqualTo(42L);
        assertThat(sent.htmlBody()).isNull();
        assertThat(sent.brevoParams()).containsEntry("tenantName", "Test League");
    }

    @Test
    void brevoHostedType_withMissingTemplateId_fallsBackToInRepoFallback() {
        when(renderer.render(eq("missing-template-fallback"), anyMap()))
                .thenReturn(new RenderedTemplate("Email not configured", "<html>fallback</html>", "fallback"));

        service.send(EmailType.MEMBERSHIP_DEPLETED,
                new EmailRecipient("s@example.com", "Ana"),
                tenantId,
                Map.of("studentName", "Ana", "programName", "Tennis"));

        var captor = org.mockito.ArgumentCaptor.forClass(OutboundEmail.class);
        verify(transport).send(captor.capture());
        assertThat(captor.getValue().brevoTemplateId()).isNull();
        assertThat(captor.getValue().htmlBody()).isEqualTo("<html>fallback</html>");
    }

    @Test
    void missingRequiredParam_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.send(
                EmailType.ACCOUNT_SETUP,
                new EmailRecipient("u@x.com", "U"),
                tenantId,
                Map.of("recipientName", "U"))) // missing role, tenantName, setupUrl, expiresAt
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required params");
    }

    @Test
    void transportThrows_exceptionIsSwallowed_noExceptionEscapes() {
        when(renderer.render(anyString(), anyMap()))
                .thenReturn(new RenderedTemplate("subj", "<html/>", "txt"));
        doThrow(new RuntimeException("Brevo is down")).when(transport).send(any());

        assertThatCode(() -> service.send(
                EmailType.ACCOUNT_SETUP,
                new EmailRecipient("u@x.com", "U"),
                tenantId,
                Map.of("recipientName", "U", "role", "student", "tenantName", "T",
                       "setupUrl", "https://x.com", "expiresAt", "2026-05-01")))
                .doesNotThrowAnyException();
    }

    @Test
    void eachSendCallGeneratesDistinctIdempotencyKey() {
        when(renderer.render(anyString(), anyMap()))
                .thenReturn(new RenderedTemplate("s", "<h/>", "t"));
        Map<String, Object> params = Map.of("recipientName", "n", "role", "student",
                "tenantName", "T", "setupUrl", "u", "expiresAt", "e");

        service.send(EmailType.ACCOUNT_SETUP, new EmailRecipient("a@x.com", "A"), tenantId, params);
        service.send(EmailType.ACCOUNT_SETUP, new EmailRecipient("b@x.com", "B"), tenantId, params);

        var captor = org.mockito.ArgumentCaptor.forClass(OutboundEmail.class);
        verify(transport, times(2)).send(captor.capture());
        List<OutboundEmail> sent = captor.getAllValues();
        assertThat(sent.get(0).idempotencyKey()).isNotEqualTo(sent.get(1).idempotencyKey());
    }
}
