package com.klasio.email.infrastructure.transport;

import com.klasio.email.domain.model.OutboundEmail;
import com.klasio.email.domain.port.EmailTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Slf4j
@Component
@ConditionalOnProperty(name = "klasio.email.transport", havingValue = "logging")
public class LoggingEmailTransport implements EmailTransport {

    private static final Path PREVIEW_DIR = Path.of("local/email-previews");

    @Override
    public void send(OutboundEmail email) {
        log.info("[EMAIL] type={} to={} subject={} idempotencyKey={}",
                email.type(), email.to().email(), email.subject(), email.idempotencyKey());
        if (email.htmlBody() != null) {
            writePreview(email);
        } else {
            log.info("[EMAIL] Brevo-hosted templateId={} params={}",
                    email.brevoTemplateId(), email.brevoParams());
        }
    }

    private void writePreview(OutboundEmail email) {
        try {
            Files.createDirectories(PREVIEW_DIR);
            String filename = Instant.now().toString().replace(":", "-")
                    + "-" + email.type().name().toLowerCase() + ".html";
            Path file = PREVIEW_DIR.resolve(filename);
            Files.writeString(file, email.htmlBody());
            log.info("[EMAIL] Preview written to {}", file.toAbsolutePath());
        } catch (IOException e) {
            log.warn("[EMAIL] Failed to write preview: {}", e.getMessage());
        }
    }
}
