package com.klasio.shared.infrastructure.exception;

public class SlugAlreadyExistsException extends RuntimeException {

    private final String suggestedSlug;

    public SlugAlreadyExistsException(String message, String suggestedSlug) {
        super(message);
        this.suggestedSlug = suggestedSlug;
    }

    public String getSuggestedSlug() {
        return suggestedSlug;
    }
}
