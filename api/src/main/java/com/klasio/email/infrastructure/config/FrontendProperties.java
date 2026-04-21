package com.klasio.email.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "klasio.frontend")
public record FrontendProperties(String urlTemplate) {}
