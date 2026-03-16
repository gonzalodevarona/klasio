package com.klasio.tenant.domain.model;

import java.text.Normalizer;
import java.util.regex.Pattern;

public record TenantSlug(String value) {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 60;
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]+");

    public TenantSlug {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Slug must not be blank");
        }
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "Slug must be at least %d characters, got %d".formatted(MIN_LENGTH, value.length()));
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Slug must be at most %d characters, got %d".formatted(MAX_LENGTH, value.length()));
        }
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Slug must match pattern %s, got '%s'".formatted(VALID_PATTERN.pattern(), value));
        }
    }

    public static TenantSlug fromName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }

        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        String lowered = withoutDiacritics.toLowerCase();
        String slug = NON_ALPHANUMERIC_PATTERN.matcher(lowered).replaceAll("-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");

        return new TenantSlug(slug);
    }
}
