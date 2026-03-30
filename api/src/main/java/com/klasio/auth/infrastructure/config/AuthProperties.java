package com.klasio.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "klasio.auth")
public record AuthProperties(
        int emailVerificationExpiryHours,
        int passwordResetExpiryMinutes,
        int maxFailedLoginAttempts,
        int lockoutDurationMinutes,
        String fromEmail
) {}
