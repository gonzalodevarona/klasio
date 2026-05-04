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
        assertThat(result.htmlBody()).contains("Cancelled");
        assertThat(result.htmlBody()).doesNotContain("CANCELLED");
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
        assertThat(result.htmlBody()).contains("Alert");
        assertThat(result.htmlBody()).doesNotContain("ALERTED");
        assertThat(result.htmlBody()).doesNotContain("Reason");
    }

    @Test
    void missingTemplateFallback_showsTypeNameForDebugging() {
        RenderedTemplate result = renderer.render("missing-template-fallback", Locale.ENGLISH, Map.of(
                "tenantName", "Test League",
                "emailTypeName", "SOME_UNREGISTERED_TYPE",
                "tenantSlug", "test-league",
                "loginUrl", "http://localhost:3000"));

        assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
        assertThat(result.htmlBody()).contains("DM Sans");
        assertThat(result.htmlBody()).contains("Test League");
        assertThat(result.htmlBody()).contains("SOME_UNREGISTERED_TYPE");
    }

    @Test
    void header_state1_tenantWithLogo_rendersImgWithLogoUrl() {
        RenderedTemplate r = renderer.render("password-recovery", Locale.ENGLISH, Map.of(
                "resetUrl", "http://e", "expiresAt", "x",
                "tenantName", "Acme League", "tenantSlug", "acme",
                "tenantLogoUrl", "https://cdn.example.com/logos/acme.png",
                "loginUrl", "http://e"));

        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(r.htmlBody());
        org.jsoup.select.Elements imgs = doc.select(
                "img[src='https://cdn.example.com/logos/acme.png']");
        assertThat(imgs).as("expected tenant logo <img>").isNotEmpty();
        assertThat(r.htmlBody()).contains("Acme League");
    }

    @Test
    void header_state2_tenantWithoutLogo_rendersKlasioBadgeAndTenantName() {
        RenderedTemplate r = renderer.render("password-recovery", Locale.ENGLISH, Map.of(
                "resetUrl", "http://e", "expiresAt", "x",
                "tenantName", "Acme League", "tenantSlug", "acme",
                "loginUrl", "http://e"));
        // tenantLogoUrl intentionally absent (treated as null by Thymeleaf).

        assertThat(r.htmlBody())
                .as("Klasio badge SVG path must be present")
                .contains("M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z");
        assertThat(r.htmlBody()).contains("Acme League");
        assertThat(r.htmlBody()).doesNotContain(">Klasio<");
    }

    @Test
    void header_state3_noTenant_rendersKlasioBadgeAndKlasioWordmark() {
        RenderedTemplate r = renderer.render("password-recovery", Locale.ENGLISH, Map.of(
                "resetUrl", "http://e", "expiresAt", "x",
                "tenantSlug", "system",
                "loginUrl", "http://e"));
        // Both tenantName and tenantLogoUrl absent.

        assertThat(r.htmlBody()).contains("M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z");
        assertThat(r.htmlBody()).contains(">Klasio<");
    }

    @Test
    void allTemplates_haveNoBareTextNodeInHeadOutsideTitle() {
        java.util.Map<String, java.util.Map<String, Object>> samples = new java.util.LinkedHashMap<>();
        samples.put("account-setup", java.util.Map.of(
                "recipientName", "X", "role", "student",
                "setupUrl", "http://e", "expiresAt", "x",
                "tenantName", "T", "tenantSlug", "t", "loginUrl", "http://e"));
        samples.put("professor-invitation", java.util.Map.of(
                "professorName", "X", "activationUrl", "http://e",
                "expiresAt", "x", "tenantName", "T",
                "tenantSlug", "t", "loginUrl", "http://e"));
        samples.put("password-recovery", java.util.Map.of(
                "resetUrl", "http://e", "expiresAt", "x",
                "tenantName", "T", "tenantSlug", "t", "loginUrl", "http://e"));
        samples.put("payment-proof-uploaded", java.util.Map.of(
                "studentName", "X", "programName", "P",
                "reviewUrl", "http://e", "tenantName", "T",
                "tenantSlug", "t", "loginUrl", "http://e"));
        samples.put("payment-rejected", java.util.Map.of(
                "studentName", "X", "programName", "P", "reason", "r",
                "retryUrl", "http://e", "tenantName", "T",
                "tenantSlug", "t", "loginUrl", "http://e"));
        samples.put("membership-activated", java.util.Map.of(
                "studentName", "X", "programName", "P", "planName", "PL",
                "totalHours", 1, "expiresAt", "2026-05-31",
                "tenantName", "T", "tenantSlug", "t", "loginUrl", "http://e"));
        samples.put("membership-expiry-warning", java.util.Map.of(
                "studentName", "X", "programName", "P", "remainingHours", 1,
                "expiresAt", "2026-05-31", "tenantName", "T",
                "tenantSlug", "t", "loginUrl", "http://e"));
        samples.put("membership-depleted", java.util.Map.of(
                "studentName", "X", "programName", "P", "tenantName", "T",
                "tenantSlug", "t", "loginUrl", "http://e"));
        samples.put("class-session-change", java.util.Map.of(
                "studentName", "X", "className", "C", "startsAt", "x",
                "changeKind", "ALERTED", "reason", "r",
                "tenantName", "T", "tenantSlug", "t", "loginUrl", "http://e"));
        samples.put("missing-template-fallback", java.util.Map.of(
                "tenantName", "T", "emailTypeName", "SOME_TYPE",
                "tenantSlug", "t", "loginUrl", "http://e"));

        samples.forEach((tpl, model) -> {
            RenderedTemplate r = renderer.render(tpl, Locale.ENGLISH, model);
            String html = r.htmlBody();
            int headOpen = html.indexOf("<head");
            int headClose = html.indexOf("</head>");
            if (headOpen < 0 || headClose < 0) return;
            String headSection = html.substring(headOpen, headClose + 7);
            // Strip <title> content (allowed opaque element that hides text)
            String withoutTitle = headSection.replaceAll("(?s)<title[^>]*>.*?</title>", "");
            // Strip all remaining tags
            String bareText = withoutTitle.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
            assertThat(bareText)
                .as("template '%s' has bare text in <head> outside <title>: '%s'", tpl, bareText)
                .isEmpty();
        });
    }
}
