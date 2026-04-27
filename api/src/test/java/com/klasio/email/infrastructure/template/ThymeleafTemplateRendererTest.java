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
}
