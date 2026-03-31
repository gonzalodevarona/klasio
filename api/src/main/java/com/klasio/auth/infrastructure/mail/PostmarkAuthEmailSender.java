package com.klasio.auth.infrastructure.mail;

import com.klasio.auth.application.port.AuthEmailSender;
import com.klasio.auth.infrastructure.config.AuthProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class PostmarkAuthEmailSender implements AuthEmailSender {

    private final JavaMailSender mailSender;
    private final AuthProperties authProperties;

    public PostmarkAuthEmailSender(JavaMailSender mailSender, AuthProperties authProperties) {
        this.mailSender = mailSender;
        this.authProperties = authProperties;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String rawToken, String tenantSlug) {
        String verificationUrl = "http://localhost:3000/verify-email?token=" + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(authProperties.fromEmail());
        message.setTo(toEmail);
        message.setSubject("Verify your Klasio account");
        message.setText("""
                Welcome to Klasio!

                Please verify your email address by clicking the link below:

                %s

                This link will expire in %d hours.

                If you did not create an account, please ignore this email.
                """.formatted(verificationUrl, authProperties.emailVerificationExpiryHours()));

        mailSender.send(message);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        String resetUrl = "http://localhost:3000/reset-password?token=" + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(authProperties.fromEmail());
        message.setTo(toEmail);
        message.setSubject("Reset your Klasio password");
        message.setText("""
                You requested a password reset for your Klasio account.

                Click the link below to set a new password:

                %s

                This link will expire in %d minutes.

                If you did not request a password reset, please ignore this email.
                """.formatted(resetUrl, authProperties.passwordResetExpiryMinutes()));

        mailSender.send(message);
    }
}
