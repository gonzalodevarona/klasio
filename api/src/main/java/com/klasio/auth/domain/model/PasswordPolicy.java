package com.klasio.auth.domain.model;

import java.util.ArrayList;
import java.util.List;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;

    private PasswordPolicy() {}

    public static List<String> validate(String password) {
        List<String> violations = new ArrayList<>();

        if (password == null || password.length() < MIN_LENGTH) {
            violations.add("MINIMUM_LENGTH");
        }
        if (password == null || !password.chars().anyMatch(Character::isUpperCase)) {
            violations.add("REQUIRES_UPPERCASE");
        }
        if (password == null || !password.chars().anyMatch(Character::isDigit)) {
            violations.add("REQUIRES_DIGIT");
        }
        if (password == null || !password.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(c) >= 0)) {
            violations.add("REQUIRES_SPECIAL_CHAR");
        }

        return violations;
    }

    public static boolean isSatisfiedBy(String password) {
        return validate(password).isEmpty();
    }
}
