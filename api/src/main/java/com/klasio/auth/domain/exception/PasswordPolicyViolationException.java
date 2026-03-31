package com.klasio.auth.domain.exception;

import java.util.List;

public class PasswordPolicyViolationException extends RuntimeException {
    private final List<String> violations;

    public PasswordPolicyViolationException(List<String> violations) {
        super("Password does not meet requirements");
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
