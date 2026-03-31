package com.klasio.auth.application.port;

public interface AuthEmailSender {

    void sendVerificationEmail(String toEmail, String rawToken, String tenantSlug);

    void sendPasswordResetEmail(String toEmail, String rawToken);
}
