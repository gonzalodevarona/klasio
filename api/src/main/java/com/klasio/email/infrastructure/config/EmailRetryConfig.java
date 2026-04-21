package com.klasio.email.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
@EnableConfigurationProperties({EmailProperties.class, BrevoProperties.class, FrontendProperties.class})
public class EmailRetryConfig {}
