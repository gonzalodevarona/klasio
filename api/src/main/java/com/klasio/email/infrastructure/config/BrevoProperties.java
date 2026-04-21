package com.klasio.email.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Collections;
import java.util.Map;

@ConfigurationProperties(prefix = "klasio.email.brevo")
public record BrevoProperties(
        String apiKey,
        String apiBaseUrl,
        Map<String, Long> templateIds
) {
    public BrevoProperties {
        templateIds = templateIds == null ? Collections.emptyMap() : templateIds;
    }
}
