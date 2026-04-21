package com.klasio.email.infrastructure.transport;

import com.klasio.email.domain.model.OutboundEmail;
import com.klasio.email.domain.port.EmailTransport;
import com.klasio.email.infrastructure.config.BrevoProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Brevo (formerly Sendinblue) email transport.
 *
 * <p>Retry policy: up to 3 attempts with exponential backoff (1s → 5s → 25s, capped at 30s)
 * for 5xx errors, 429 Too Many Requests, and transient I/O failures.
 * After all retries are exhausted, the failure is logged and silently swallowed so that
 * email delivery never blocks the business operation that triggered it.
 *
 * <p>Retry logic is implemented via {@link RetryTemplate} directly in the method body (not via
 * {@code @Retryable} annotation) so that the behavior is testable without a Spring context.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "klasio.email.transport", havingValue = "brevo")
public class BrevoEmailTransport implements EmailTransport {

    private final BrevoProperties props;
    private final RestClient restClient;
    private final RetryTemplate retryTemplate;

    public BrevoEmailTransport(BrevoProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.apiBaseUrl())
                .defaultHeader("api-key", props.apiKey() != null ? props.apiKey() : "")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.retryTemplate = buildRetryTemplate();
    }

    @PostConstruct
    void validate() {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "BREVO_API_KEY is required when klasio.email.transport=brevo");
        }
    }

    @Override
    public void send(OutboundEmail email) {
        try {
            retryTemplate.execute((RetryCallback<Void, Exception>) context -> {
                doSend(email);
                return null;
            });
        } catch (Exception ex) {
            // All retries exhausted — log and swallow so email never blocks business flow
            log.error("[EMAIL] All retries exhausted — type={} to={} error={}",
                    email.type(), email.to().email(), ex.getMessage());
        }
    }

    // ---------- private helpers ----------

    private void doSend(OutboundEmail email) {
        Map<?, ?> response = restClient.post()
                .uri("/smtp/email")
                .header("Idempotency-Key", email.idempotencyKey())
                .body(buildBody(email))
                .retrieve()
                .body(Map.class);

        String messageId = response != null ? (String) response.get("messageId") : "unknown";
        log.info("[EMAIL] Sent type={} to={} brevoMessageId={}",
                email.type(), email.to().email(), messageId);
    }

    private Map<String, Object> buildBody(OutboundEmail email) {
        Map<String, Object> body = new HashMap<>();
        body.put("sender", Map.of("name", email.from().displayName(), "email", email.from().email()));
        body.put("to", List.of(Map.of("email", email.to().email(), "name", email.to().displayName())));

        if (email.brevoTemplateId() != null) {
            // Brevo-hosted template path: send templateId + params, no HTML
            body.put("templateId", email.brevoTemplateId());
            body.put("params", email.brevoParams() != null ? email.brevoParams() : Map.of());
        } else {
            // In-repo template path: send rendered HTML + subject
            body.put("subject", email.subject());
            body.put("htmlContent", email.htmlBody());
            if (email.textBody() != null) {
                body.put("textContent", email.textBody());
            }
        }
        return body;
    }

    /**
     * Builds a RetryTemplate that retries on 5xx, 429, and I/O errors with
     * exponential backoff: 1s initial, multiplier 5, max 30s, up to 3 attempts.
     */
    private static RetryTemplate buildRetryTemplate() {
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(HttpServerErrorException.class, true);
        retryableExceptions.put(HttpClientErrorException.TooManyRequests.class, true);
        retryableExceptions.put(ResourceAccessException.class, true);
        retryableExceptions.put(IOException.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions, true);

        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(5.0);
        backOff.setMaxInterval(30000L);

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOff);
        return template;
    }
}
