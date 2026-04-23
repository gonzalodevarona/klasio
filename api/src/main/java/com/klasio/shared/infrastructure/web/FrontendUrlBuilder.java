package com.klasio.shared.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FrontendUrlBuilder {

    private final String urlTemplate;

    public FrontendUrlBuilder(
            @Value("${klasio.frontend.url-template:http://localhost:3000}") String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public String build(String tenantSlug, String path) {
        String base = urlTemplate.contains("{tenantSlug}")
                ? urlTemplate.replace("{tenantSlug}", tenantSlug)
                : urlTemplate;
        return base + (path.startsWith("/") ? path : "/" + path);
    }
}
