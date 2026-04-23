package com.klasio.email.application;

import java.util.Map;
import java.util.UUID;

public interface EmailService {
    void send(EmailType type, EmailRecipient to, UUID tenantId, Map<String, Object> params);
}
