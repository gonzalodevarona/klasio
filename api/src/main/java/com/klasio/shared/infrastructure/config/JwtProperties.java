package com.klasio.shared.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "klasio.jwt")
public record JwtProperties(
        String secret,
        long expiration
) {
}
