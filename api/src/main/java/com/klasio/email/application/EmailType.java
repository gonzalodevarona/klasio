package com.klasio.email.application;

import java.util.Set;

public enum EmailType {
    ACCOUNT_SETUP(Source.IN_REPO, "account-setup",
            Set.of("recipientName", "role", "tenantName", "setupUrl", "expiresAt")),
    PROFESSOR_INVITATION(Source.IN_REPO, "professor-invitation",
            Set.of("activationUrl", "expiresAt", "professorName")),
    PASSWORD_RECOVERY(Source.IN_REPO, "password-recovery",
            Set.of("resetUrl", "expiresAt")),
    PAYMENT_PROOF_UPLOADED(Source.IN_REPO, "payment-proof-uploaded",
            Set.of("studentName", "programName", "reviewUrl")),
    PAYMENT_REJECTED(Source.IN_REPO, "payment-rejected",
            Set.of("studentName", "programName", "reason", "retryUrl")),
    MEMBERSHIP_ACTIVATED(Source.BREVO_HOSTED, "membership-activated",
            Set.of("studentName", "programName", "planName", "totalHours", "expiresAt")),
    MEMBERSHIP_EXPIRY_WARNING(Source.BREVO_HOSTED, "membership-expiry-warning",
            Set.of("studentName", "programName", "expiresAt", "remainingHours")),
    MEMBERSHIP_DEPLETED(Source.BREVO_HOSTED, "membership-depleted",
            Set.of("studentName", "programName")),
    CLASS_SESSION_CHANGE(Source.BREVO_HOSTED, "class-session-change",
            Set.of("studentName", "className", "startsAt", "changeKind", "reason"));

    public enum Source { IN_REPO, BREVO_HOSTED }

    private final Source source;
    private final String templateRef;
    private final Set<String> requiredKeys;

    EmailType(Source source, String templateRef, Set<String> requiredKeys) {
        this.source = source;
        this.templateRef = templateRef;
        this.requiredKeys = requiredKeys;
    }

    public Source source()            { return source; }
    public String templateRef()       { return templateRef; }
    public Set<String> requiredKeys() { return requiredKeys; }
}
