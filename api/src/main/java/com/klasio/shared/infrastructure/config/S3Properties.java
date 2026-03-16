package com.klasio.shared.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "klasio.s3")
public record S3Properties(
        String endpoint,
        String region,
        String bucket
) {
}
