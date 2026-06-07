package com.klasio.auth.domain.exception;

public class SelfRegistrationConflictException extends RuntimeException {
    public SelfRegistrationConflictException() {
        super("Registration could not be completed");
    }
}
