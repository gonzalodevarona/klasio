package com.klasio.email.infrastructure.service;

import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailType;
import com.klasio.email.domain.model.*;
import com.klasio.email.domain.port.EmailTransport;
import com.klasio.email.domain.port.TemplateRenderer;
import com.klasio.email.domain.port.TenantContextPort;
import com.klasio.email.infrastructure.config.EmailProperties;
import com.klasio.email.infrastructure.config.FrontendProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
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

    private final FrontendProperties frontendProps = new FrontendProperties("http://localhost:3000");

    private EmailDispatcherService service;

    private final UUID tenantId = UUID.randomUUID();
    private final TenantContext tenant = new TenantContext(tenantId, "test-league", "Test League", "en", "America/Bogota");

    @BeforeEach
    void setUp() {
        when(tenantContextPort.findById(tenantId)).thenReturn(tenant);
        service = new EmailDispatcherService(transport, renderer, tenantContextPort, props, frontendProps);
    }

    @Test
    void rendersTemplateWithLocaleAndPassesToTransport() {
        when(renderer.render(eq("account-setup"), eq(Locale.ENGLISH), anyMap()))
                .thenReturn(new RenderedTemplate("Set up your account", "<html/>", "plain text"));

        service.send(EmailType.ACCOUNT_SETUP,
                new EmailRecipient("user@example.com", "Juan"),
                tenantId,
                Map.of("recipientName", "Juan", "role", "student",
                       "setupUrl", "https://x.com", "expiresAt", "2026-05-01"));

        var captor = org.mockito.ArgumentCaptor.forClass(OutboundEmail.class);
        verify(transport).send(captor.capture());
        OutboundEmail sent = captor.getValue();
        assertThat(sent.type()).isEqualTo(EmailType.ACCOUNT_SETUP);
        assertThat(sent.subject()).isEqualTo("Set up your account");
        assertThat(sent.htmlBody()).isEqualTo("<html/>");
        assertThat(sent.from().email()).isEqualTo("noreply@klasio.app");
        assertThat(sent.from().displayName()).isEqualTo("Test League via Klasio");
    }

    @Test
    void localeResolvesFromTenantLanguage() {
        TenantContext spanishTenant = new TenantContext(tenantId, "liga", "Liga Bogotá", "es", "America/Bogota");
        when(tenantContextPort.findById(tenantId)).thenReturn(spanishTenant);
        service = new EmailDispatcherService(transport, renderer, tenantContextPort, props, frontendProps);

        when(renderer.render(eq("account-setup"), eq(Locale.forLanguageTag("es")), anyMap()))
                .thenReturn(new RenderedTemplate("Configura tu cuenta", "<html/>", "texto"));

        service.send(EmailType.ACCOUNT_SETUP,
                new EmailRecipient("u@x.com", "U"),
                tenantId,
                Map.of("recipientName", "U", "role", "student",
                       "setupUrl", "u", "expiresAt", "e"));

        verify(renderer).render(eq("account-setup"), eq(Locale.forLanguageTag("es")), anyMap());
    }

    @Test
    void modelContainsTenantNameSlugAndLoginUrl() {
        when(renderer.render(anyString(), any(Locale.class), anyMap()))
                .thenReturn(new RenderedTemplate("s", "<h/>", "t"));

        service.send(EmailType.ACCOUNT_SETUP,
                new EmailRecipient("u@x.com", "U"),
                tenantId,
                Map.of("recipientName", "U", "role", "student",
                       "setupUrl", "u", "expiresAt", "e"));

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Map<String, Object>> modelCaptor =
                (org.mockito.ArgumentCaptor<Map<String, Object>>) (org.mockito.ArgumentCaptor<?>)
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(renderer).render(anyString(), any(Locale.class), modelCaptor.capture());
        Map<String, Object> model = modelCaptor.getValue();
        assertThat(model).containsKey("tenantName");
        assertThat(model).containsKey("tenantSlug");
        assertThat(model).containsKey("loginUrl");
        assertThat(model.get("loginUrl")).isEqualTo("http://localhost:3000");
    }

    @Test
    void missingRequiredParam_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.send(
                EmailType.ACCOUNT_SETUP,
                new EmailRecipient("u@x.com", "U"),
                tenantId,
                Map.of("recipientName", "U")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required params");
    }

    @Test
    void transportThrows_exceptionIsSwallowed_noExceptionEscapes() {
        when(renderer.render(anyString(), any(Locale.class), anyMap()))
                .thenReturn(new RenderedTemplate("subj", "<html/>", "txt"));
        doThrow(new RuntimeException("Brevo is down")).when(transport).send(any());

        assertThatCode(() -> service.send(
                EmailType.ACCOUNT_SETUP,
                new EmailRecipient("u@x.com", "U"),
                tenantId,
                Map.of("recipientName", "U", "role", "student",
                       "setupUrl", "https://x.com", "expiresAt", "2026-05-01")))
                .doesNotThrowAnyException();
    }

    @Test
    void eachSendCallGeneratesDistinctIdempotencyKey() {
        when(renderer.render(anyString(), any(Locale.class), anyMap()))
                .thenReturn(new RenderedTemplate("s", "<h/>", "t"));
        Map<String, Object> params = Map.of("recipientName", "n", "role", "student",
                "setupUrl", "u", "expiresAt", "e");

        service.send(EmailType.ACCOUNT_SETUP, new EmailRecipient("a@x.com", "A"), tenantId, params);
        service.send(EmailType.ACCOUNT_SETUP, new EmailRecipient("b@x.com", "B"), tenantId, params);

        var captor = org.mockito.ArgumentCaptor.forClass(OutboundEmail.class);
        verify(transport, times(2)).send(captor.capture());
        List<OutboundEmail> sent = captor.getAllValues();
        assertThat(sent.get(0).idempotencyKey()).isNotEqualTo(sent.get(1).idempotencyKey());
    }

    @Test
    void instantParamIsFormattedUsingTenantTimezone() {
        TenantContext tzTenant = new TenantContext(tenantId, "test-league", "Test League", "en", "America/Bogota");
        when(tenantContextPort.findById(tenantId)).thenReturn(tzTenant);
        service = new EmailDispatcherService(transport, renderer, tenantContextPort, props, frontendProps);

        when(renderer.render(anyString(), any(Locale.class), anyMap()))
                .thenReturn(new RenderedTemplate("s", "<h/>", "t"));

        Instant expiresAt = java.time.Instant.parse("2026-04-27T23:43:04Z"); // 18:43 Bogota (UTC-5)

        service.send(EmailType.ACCOUNT_SETUP,
                new EmailRecipient("u@x.com", "U"),
                tenantId,
                new java.util.HashMap<>(Map.of(
                        "recipientName", "U", "role", "student",
                        "setupUrl", "u", "expiresAt", expiresAt)));

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Map<String, Object>> captor =
                (org.mockito.ArgumentCaptor<Map<String, Object>>) (org.mockito.ArgumentCaptor<?>)
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(renderer).render(anyString(), any(Locale.class), captor.capture());
        assertThat(captor.getValue().get("expiresAt")).isEqualTo("27/04/2026 6:43 PM");
    }

    @Test
    void localDateParamIsFormattedAsDdMmYyyy() {
        when(renderer.render(anyString(), any(Locale.class), anyMap()))
                .thenReturn(new RenderedTemplate("s", "<h/>", "t"));

        java.time.LocalDate expiresAt = java.time.LocalDate.of(2026, 5, 31);

        service.send(EmailType.ACCOUNT_SETUP,
                new EmailRecipient("u@x.com", "U"),
                tenantId,
                new java.util.HashMap<>(Map.of(
                        "recipientName", "U", "role", "student",
                        "setupUrl", "u", "expiresAt", expiresAt)));

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Map<String, Object>> captor =
                (org.mockito.ArgumentCaptor<Map<String, Object>>) (org.mockito.ArgumentCaptor<?>)
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(renderer).render(anyString(), any(Locale.class), captor.capture());
        assertThat(captor.getValue().get("expiresAt")).isEqualTo("31/05/2026");
    }
}
