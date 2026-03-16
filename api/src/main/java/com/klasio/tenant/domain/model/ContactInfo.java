package com.klasio.tenant.domain.model;

import java.util.regex.Pattern;

public record ContactInfo(String email, String phone, String address) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public ContactInfo {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Contact email must not be blank");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Contact email has invalid format: '%s'".formatted(email));
        }
    }
}
