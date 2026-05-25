package com.klasio.email.application;

import java.util.Set;

public enum EmailType {
    ACCOUNT_SETUP("account-setup",
            Set.of("recipientName", "role", "setupUrl", "expiresAt")),
    PROFESSOR_INVITATION("professor-invitation",
            Set.of("activationUrl", "expiresAt", "professorName")),
    PASSWORD_RECOVERY("password-recovery",
            Set.of("resetUrl", "expiresAt")),
    PAYMENT_PROOF_UPLOADED("payment-proof-uploaded",
            Set.of("studentName", "programName", "reviewUrl")),
    PAYMENT_REJECTED("payment-rejected",
            Set.of("studentName", "programName", "reason", "retryUrl")),
    MEMBERSHIP_ACTIVATED("membership-activated",
            Set.of("studentName", "programName", "planName", "totalHours", "expiresAt")),
    MEMBERSHIP_EXPIRY_WARNING("membership-expiry-warning",
            Set.of("studentName", "programName", "expiresAt", "remainingHours")),
    MEMBERSHIP_DEPLETED("membership-depleted",
            Set.of("studentName", "programName")),
    MEMBERSHIP_LOW_HOURS("membership-low-hours",
            Set.of("studentName", "remainingHours")),
    CLASS_SESSION_CHANGE("class-session-change",
            Set.of("studentName", "className", "startsAt", "changeKind", "reason"));

    private final String templateRef;
    private final Set<String> requiredKeys;

    EmailType(String templateRef, Set<String> requiredKeys) {
        this.templateRef = templateRef;
        this.requiredKeys = requiredKeys;
    }

    public String templateRef()       { return templateRef; }
    public Set<String> requiredKeys() { return requiredKeys; }
}
