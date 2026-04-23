package com.klasio.tenant.domain.model;

import java.util.regex.Pattern;

public record ContactInfo(
        String email,
        String phone,
        String phoneIndicator,
        String street,
        String city,
        String state,
        String country
) {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+$");

    public ContactInfo {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Contact email must not be blank");
        if (!EMAIL_PATTERN.matcher(email).matches())
            throw new IllegalArgumentException("Contact email has invalid format: '%s'".formatted(email));
        if (phone == null || phone.isBlank())
            throw new IllegalArgumentException("Contact phone must not be blank");
        if (!DIGITS_ONLY.matcher(phone).matches())
            throw new IllegalArgumentException("Contact phone must contain digits only");
        if (phoneIndicator == null || phoneIndicator.isBlank())
            throw new IllegalArgumentException("Contact phone indicator must not be blank");
        if (street == null || street.isBlank())
            throw new IllegalArgumentException("Contact street must not be blank");
        if (city == null || city.isBlank())
            throw new IllegalArgumentException("Contact city must not be blank");
        if (state == null || state.isBlank())
            throw new IllegalArgumentException("Contact state must not be blank");
        if (country == null || country.isBlank())
            throw new IllegalArgumentException("Contact country must not be blank");
    }
}
