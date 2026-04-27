package com.klasio.email.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "klasio.email.brevo")
public record BrevoProperties(String apiKey, String apiBaseUrl) {}
