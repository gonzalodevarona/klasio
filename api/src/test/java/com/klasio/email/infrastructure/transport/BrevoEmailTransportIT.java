package com.klasio.email.infrastructure.transport;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailType;
import com.klasio.email.domain.model.EmailSender;
import com.klasio.email.domain.model.OutboundEmail;
import com.klasio.email.infrastructure.config.BrevoProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.*;

class BrevoEmailTransportIT {

    static WireMockServer wireMock = new WireMockServer(options().port(9877));

    @BeforeAll static void start() { wireMock.start(); WireMock.configureFor(9877); }
    @AfterAll  static void stop()  { wireMock.stop(); }
    @BeforeEach void reset()       { wireMock.resetAll(); }

    private BrevoEmailTransport transport() {
        BrevoProperties props = new BrevoProperties(
                "test-api-key", "http://localhost:9877", Map.of());
        return new BrevoEmailTransport(props);
    }

    @Test
    void inRepoEmail_sendsHtmlContentAndSubjectWithIdempotencyKey() {
        stubFor(post(urlEqualTo("/smtp/email"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"messageId\":\"<fake@brevo.com>\"}")));

        transport().send(new OutboundEmail(
                EmailType.STUDENT_VERIFICATION,
                new EmailRecipient("user@example.com", "Juan"),
                new EmailSender("noreply@klasio.app", "Test League via Klasio"),
                "Verify your account", "<html>body</html>", "plain text",
                null, null, "idem-key-123"));

        verify(postRequestedFor(urlEqualTo("/smtp/email"))
                .withHeader("api-key", equalTo("test-api-key"))
                .withHeader("Idempotency-Key", equalTo("idem-key-123"))
                .withRequestBody(matchingJsonPath("$.sender.email", equalTo("noreply@klasio.app")))
                .withRequestBody(matchingJsonPath("$.sender.name", equalTo("Test League via Klasio")))
                .withRequestBody(matchingJsonPath("$.to[0].email", equalTo("user@example.com")))
                .withRequestBody(matchingJsonPath("$.subject", equalTo("Verify your account")))
                .withRequestBody(matchingJsonPath("$.htmlContent"))
                .withRequestBody(matchingJsonPath("$.textContent")));

        // Brevo-hosted fields must NOT be present for in-repo emails
        verify(postRequestedFor(urlEqualTo("/smtp/email"))
                .withRequestBody(notMatching(".*\"templateId\".*")));
    }

    @Test
    void brevoHostedEmail_sendsTemplateIdAndParamsNoHtmlContent() {
        stubFor(post(urlEqualTo("/smtp/email"))
                .willReturn(aResponse().withStatus(201)
                        .withBody("{\"messageId\":\"<fake2@brevo.com>\"}")));

        transport().send(new OutboundEmail(
                EmailType.MEMBERSHIP_ACTIVATED,
                new EmailRecipient("student@example.com", "Ana"),
                new EmailSender("noreply@klasio.app", "Tennis Club via Klasio"),
                null, null, null,
                42L, Map.of("studentName", "Ana", "tenantName", "Tennis Club"),
                "idem-key-456"));

        verify(postRequestedFor(urlEqualTo("/smtp/email"))
                .withHeader("Idempotency-Key", equalTo("idem-key-456"))
                .withRequestBody(matchingJsonPath("$.templateId", equalTo("42")))
                .withRequestBody(matchingJsonPath("$.params.studentName", equalTo("Ana")))
                .withRequestBody(notMatching(".*\"htmlContent\".*")));
    }

    @Test
    void on5xxError_retriesAndEventuallySucceeds() {
        stubFor(post(urlEqualTo("/smtp/email"))
                .inScenario("retry").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("SECOND"));
        stubFor(post(urlEqualTo("/smtp/email"))
                .inScenario("retry").whenScenarioStateIs("SECOND")
                .willReturn(aResponse().withStatus(201)
                        .withBody("{\"messageId\":\"ok\"}")));

        assertThatCode(() -> transport().send(new OutboundEmail(
                EmailType.STUDENT_VERIFICATION,
                new EmailRecipient("u@x.com", "U"),
                new EmailSender("from@x.com", "League via Klasio"),
                "subj", "<h/>", "t", null, null, "idem")))
                .doesNotThrowAnyException();

        verify(2, postRequestedFor(urlEqualTo("/smtp/email")));
    }

    @Test
    void afterAllRetriesExhausted_noExceptionPropagates() {
        stubFor(post(urlEqualTo("/smtp/email"))
                .willReturn(aResponse().withStatus(500)));

        assertThatCode(() -> transport().send(new OutboundEmail(
                EmailType.STUDENT_VERIFICATION,
                new EmailRecipient("u@x.com", "U"),
                new EmailSender("from@x.com", "L via Klasio"),
                "s", "<h/>", "t", null, null, "idem")))
                .doesNotThrowAnyException();
    }
}
