package com.klasio.shared.infrastructure.web;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FrontendUrlBuilderTest {

    @Test
    void withSubdomainTemplate_insertsSlugAsSubdomain() {
        FrontendUrlBuilder builder = new FrontendUrlBuilder("https://{tenantSlug}.app.klasio.com");
        assertThat(builder.build("liga-valle-tenis", "/verify-email?token=abc"))
                .isEqualTo("https://liga-valle-tenis.app.klasio.com/verify-email?token=abc");
    }

    @Test
    void withLocalTemplate_noPlaceholder_appendsPath() {
        FrontendUrlBuilder builder = new FrontendUrlBuilder("http://localhost:3000");
        assertThat(builder.build("any-slug", "/reset-password?token=xyz"))
                .isEqualTo("http://localhost:3000/reset-password?token=xyz");
    }

    @Test
    void pathWithoutLeadingSlash_isHandled() {
        FrontendUrlBuilder builder = new FrontendUrlBuilder("http://localhost:3000");
        assertThat(builder.build("slug", "verify"))
                .isEqualTo("http://localhost:3000/verify");
    }
}
