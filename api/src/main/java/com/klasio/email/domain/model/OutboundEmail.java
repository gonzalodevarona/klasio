package com.klasio.email.domain.model;

import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailType;
import java.util.Map;

public record OutboundEmail(
        EmailType type,
        EmailRecipient to,
        EmailSender from,
        String subject,
        String htmlBody,
        String textBody,
        Long brevoTemplateId,
        Map<String, Object> brevoParams,
        String idempotencyKey
) {}
