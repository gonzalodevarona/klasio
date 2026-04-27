package com.klasio.email.infrastructure.template;

import com.klasio.email.domain.model.RenderedTemplate;
import com.klasio.email.infrastructure.config.EmailConfig;
import com.klasio.email.infrastructure.config.ThymeleafEmailConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = {EmailConfig.class, ThymeleafEmailConfig.class, ThymeleafTemplateRenderer.class})
@TestPropertySource(properties = "spring.main.web-application-type=none")
class ThymeleafTemplateRendererTest {

    @Autowired
    ThymeleafTemplateRenderer renderer;

    @Test
    void passwordRecovery_rendersInEnglishWithoutUnresolvedVariables() {
        RenderedTemplate result = renderer.render("password-recovery", Locale.ENGLISH, Map.of(
                "resetUrl", "https://test.klasio.com/reset?token=xyz",
                "expiresAt", "30 minutes",
                "tenantName", "Test League",
                "tenantSlug", "test-league",
                "loginUrl", "http://localhost:3000"));

        assertThat(result.subject()).isEqualTo("Reset your Test League password");
        assertThat(result.subject()).doesNotContain("${").doesNotContain("#{");
        assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
        assertThat(result.htmlBody()).contains("https://test.klasio.com/reset?token=xyz");
        assertThat(result.textBody()).doesNotContain("${").doesNotContain("#{");
    }

    @Test
    void passwordRecovery_rendersInSpanish() {
        RenderedTemplate result = renderer.render("password-recovery", Locale.forLanguageTag("es"), Map.of(
                "resetUrl", "https://test.klasio.com/reset?token=xyz",
                "expiresAt", "30 minutos",
                "tenantName", "Liga Test",
                "tenantSlug", "liga-test",
                "loginUrl", "http://localhost:3000"));

        assertThat(result.subject()).isEqualTo("Restablecer tu contraseña en Liga Test");
        assertThat(result.htmlBody()).contains("Solicitaste restablecer");
    }

    @Test
    void accountSetup_subjectContainsTenantName() {
        RenderedTemplate result = renderer.render("account-setup", Locale.ENGLISH, Map.of(
                "recipientName", "Carlos",
                "role", "student",
                "setupUrl", "https://test.klasio.com/setup?token=abc",
                "expiresAt", "2026-05-01 09:00",
                "tenantName", "Test League",
                "tenantSlug", "test-league",
                "loginUrl", "http://localhost:3000"));

        assertThat(result.subject()).isEqualTo("Set up your account at Test League");
        assertThat(result.htmlBody()).contains("Carlos");
        assertThat(result.htmlBody()).contains("https://test.klasio.com/setup?token=abc");
        assertThat(result.textBody()).contains("Carlos");
        assertThat(result.textBody()).doesNotContain("${").doesNotContain("#{");
    }

    @Test
    void membershipActivated_rendersCorrectly() {
        RenderedTemplate result = renderer.render("membership-activated", Locale.ENGLISH, Map.of(
                "studentName", "Ana",
                "programName", "Tennis",
                "planName", "Monthly 20h",
                "totalHours", 20,
                "expiresAt", "2026-05-31",
                "tenantName", "Tennis Club",
                "tenantSlug", "tennis-club",
                "loginUrl", "http://localhost:3000"));

        assertThat(result.subject()).isEqualTo("Your membership is now active");
        assertThat(result.htmlBody()).contains("Ana");
        assertThat(result.htmlBody()).contains("Tennis");
        assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
    }

    @Test
    void accountSetup_usesNewDesignAndResolvesAllKeys() {
        RenderedTemplate result = renderer.render("account-setup", Locale.ENGLISH, Map.of(
                "recipientName", "María García",
                "role", "student",
                "setupUrl", "https://test.klasio.com/setup?token=abc",
                "expiresAt", "2026-05-01 09:00",
                "tenantName", "Test League",
                "tenantSlug", "test-league",
                "loginUrl", "http://localhost:3000"));

        assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
        assertThat(result.htmlBody()).contains("DM Sans");
        assertThat(result.htmlBody()).contains("#CAFF4D");
        assertThat(result.htmlBody()).contains("María García");
        assertThat(result.htmlBody()).contains("https://test.klasio.com/setup?token=abc");
        assertThat(result.htmlBody()).contains("Temporary link");
    }

    @Test
    void professorInvitation_rendersCorrectly() {
        RenderedTemplate result = renderer.render("professor-invitation", Locale.ENGLISH, Map.of(
                "professorName", "Carlos Ruiz",
                "activationUrl", "https://test.klasio.com/setup?token=inv123",
                "expiresAt", "2026-05-05 18:00",
                "tenantName", "Test League",
                "tenantSlug", "test-league",
                "loginUrl", "http://localhost:3000"));

        assertThat(result.subject()).isEqualTo("You have been invited to join Test League as a professor");
        assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
        assertThat(result.htmlBody()).contains("DM Sans");
        assertThat(result.htmlBody()).contains("#CAFF4D");
        assertThat(result.htmlBody()).contains("Carlos Ruiz");
        assertThat(result.htmlBody()).contains("https://test.klasio.com/setup?token=inv123");
    }

    @Test
    void passwordRecovery_usesNewDesign() {
        RenderedTemplate result = renderer.render("password-recovery", Locale.ENGLISH, Map.of(
                "resetUrl", "https://test.klasio.com/reset?token=xyz",
                "expiresAt", "30 minutes",
                "tenantName", "Test League",
                "tenantSlug", "test-league",
                "loginUrl", "http://localhost:3000"));

        assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
        assertThat(result.htmlBody()).contains("DM Sans");
        assertThat(result.htmlBody()).contains("#CAFF4D");
        assertThat(result.htmlBody()).contains("https://test.klasio.com/reset?token=xyz");
    }

    @Test
    void paymentProofUploaded_rendersInfoPanel() {
        RenderedTemplate result = renderer.render("payment-proof-uploaded", Locale.ENGLISH, Map.of(
                "studentName", "Laura Torres",
                "programName", "Boxing Beginners",
                "reviewUrl", "https://test.klasio.com/payment-proofs/42",
                "tenantName", "Test League",
                "tenantSlug", "test-league",
                "loginUrl", "http://localhost:3000"));

        assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
        assertThat(result.htmlBody()).contains("DM Sans");
        assertThat(result.htmlBody()).contains("Laura Torres");
        assertThat(result.htmlBody()).contains("Boxing Beginners");
        assertThat(result.htmlBody()).contains("Student");
        assertThat(result.htmlBody()).contains("Program");
        assertThat(result.htmlBody()).contains("https://test.klasio.com/payment-proofs/42");
    }

    @Test
    void paymentRejected_rendersReasonCallout() {
        RenderedTemplate result = renderer.render("payment-rejected", Locale.ENGLISH, Map.of(
                "studentName", "Laura Torres",
                "programName", "Boxing Beginners",
                "reason", "Image is blurry and amount is not visible.",
                "retryUrl", "https://test.klasio.com/memberships/99/upload",
                "tenantName", "Test League",
                "tenantSlug", "test-league",
                "loginUrl", "http://localhost:3000"));

        assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
        assertThat(result.htmlBody()).contains("DM Sans");
        assertThat(result.htmlBody()).contains("Laura Torres");
        assertThat(result.htmlBody()).contains("Image is blurry and amount is not visible.");
        assertThat(result.htmlBody()).contains("https://test.klasio.com/memberships/99/upload");
        assertThat(result.htmlBody()).contains("#FFF5F5");
    }

    @Test
    void membershipActivated_rendersGreenPanelWithNewDesign() {
        RenderedTemplate result = renderer.render("membership-activated", Locale.ENGLISH, Map.of(
                "studentName", "Ana",
                "programName", "Tennis",
                "planName", "Monthly 20h",
                "totalHours", 20,
                "expiresAt", "2026-05-31",
                "tenantName", "Tennis Club",
                "tenantSlug", "tennis-club",
                "loginUrl", "http://localhost:3000"));

        assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
        assertThat(result.htmlBody()).contains("DM Sans");
        assertThat(result.htmlBody()).contains("#F4FAF6");
        assertThat(result.htmlBody()).contains("Monthly 20h");
        assertThat(result.htmlBody()).contains("Available hours");
        assertThat(result.htmlBody()).contains("Expires on");
        assertThat(result.htmlBody()).contains("http://localhost:3000");
    }

    @Test
    void membershipExpiryWarning_rendersAmberPanel() {
        RenderedTemplate result = renderer.render("membership-expiry-warning", Locale.ENGLISH, Map.of(
                "studentName", "Daniela Ríos",
                "programName", "Yoga Adults",
                "remainingHours", 2,
                "expiresAt", "2026-04-30",
                "tenantName", "Test League",
                "tenantSlug", "test-league",
                "loginUrl", "http://localhost:3000"));

        assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
        assertThat(result.htmlBody()).contains("DM Sans");
        assertThat(result.htmlBody()).contains("#FFFAF0");
        assertThat(result.htmlBody()).contains("Remaining hours");
        assertThat(result.htmlBody()).contains("Expires on");
        assertThat(result.htmlBody()).contains("Daniela Ríos");
    }

    @Test
    void membershipDepleted_rendersCorrectly() {
        RenderedTemplate result = renderer.render("membership-depleted", Locale.ENGLISH, Map.of(
                "studentName", "Pedro Sánchez",
                "programName", "Karate Kids",
                "tenantName", "Test League",
                "tenantSlug", "test-league",
                "loginUrl", "http://localhost:3000"));

        assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
        assertThat(result.htmlBody()).contains("DM Sans");
        assertThat(result.htmlBody()).contains("Pedro Sánchez");
        assertThat(result.htmlBody()).contains("Karate Kids");
        assertThat(result.htmlBody()).contains("http://localhost:3000");
    }

    @Test
    void classSessionChange_withReason_rendersBothPanelRows() {
        RenderedTemplate result = renderer.render("class-session-change", Locale.ENGLISH, Map.of(
                "studentName", "Sebastián Vargas",
                "className", "Jiu-Jitsu Advanced",
                "startsAt", "Monday April 28 at 7:00 PM",
                "changeKind", "CANCELLED",
                "reason", "Professor is ill.",
                "tenantName", "Test League",
                "tenantSlug", "test-league",
                "loginUrl", "http://localhost:3000"));

        assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
        assertThat(result.htmlBody()).contains("DM Sans");
        assertThat(result.htmlBody()).contains("CANCELLED");
        assertThat(result.htmlBody()).contains("Professor is ill.");
        assertThat(result.htmlBody()).contains("Change type");
        assertThat(result.htmlBody()).contains("Reason");
    }

    @Test
    void classSessionChange_withoutReason_omitsReasonRow() {
        RenderedTemplate result = renderer.render("class-session-change", Locale.ENGLISH, Map.of(
                "studentName", "Sebastián Vargas",
                "className", "Jiu-Jitsu Advanced",
                "startsAt", "Monday April 28 at 7:00 PM",
                "changeKind", "ALERTED",
                "reason", "",
                "tenantName", "Test League",
                "tenantSlug", "test-league",
                "loginUrl", "http://localhost:3000"));

        assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
        assertThat(result.htmlBody()).contains("ALERTED");
        assertThat(result.htmlBody()).doesNotContain("Reason");
    }
}
