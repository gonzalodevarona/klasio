package com.klasio.email.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "klasio.email")
public record EmailProperties(
        String transport,
        FromProperties from,
        RetryProperties retry
) {
    public record FromProperties(String address, String nameSuffix) {}
    public record RetryProperties(int maxAttempts, long initialDelayMs, double multiplier, long maxDelayMs) {}
}
