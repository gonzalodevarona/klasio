package com.klasio.email.infrastructure.template;

import com.klasio.email.domain.model.RenderedTemplate;
import com.klasio.email.infrastructure.config.ThymeleafEmailConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = {ThymeleafEmailConfig.class, ThymeleafTemplateRenderer.class})
@TestPropertySource(properties = "spring.main.web-application-type=none")
class ThymeleafTemplateRendererTest {

    @Autowired
    ThymeleafTemplateRenderer renderer;

    @Test
    void studentVerification_extractsSubjectAndRendersBody() {
        RenderedTemplate result = renderer.render("student-verification", Map.of(
                "studentName", "Carlos",
                "verificationUrl", "https://test.klasio.com/verify?token=abc",
                "expiresAt", "2026-05-01 09:00",
                "tenantName", "Test League",
                "tenantSlug", "test-league"));

        assertThat(result.subject()).isNotBlank();
        assertThat(result.subject()).doesNotContain("${");
        assertThat(result.htmlBody()).contains("Carlos");
        assertThat(result.htmlBody()).contains("https://test.klasio.com/verify?token=abc");
        assertThat(result.textBody()).contains("Carlos");
        assertThat(result.textBody()).doesNotContain("${");
    }

    @Test
    void passwordRecovery_rendersWithoutUnresolvedVariables() {
        RenderedTemplate result = renderer.render("password-recovery", Map.of(
                "resetUrl", "https://test.klasio.com/reset?token=xyz",
                "expiresAt", "30 minutes",
                "tenantName", "Test League",
                "tenantSlug", "test-league"));

        assertThat(result.subject()).doesNotContain("${");
        assertThat(result.htmlBody()).doesNotContain("${");
        assertThat(result.textBody()).doesNotContain("${");
    }
}
