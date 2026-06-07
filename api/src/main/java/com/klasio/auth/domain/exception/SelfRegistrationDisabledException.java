package com.klasio.auth.domain.exception;

public class SelfRegistrationDisabledException extends RuntimeException {
    public SelfRegistrationDisabledException() {
        super("Self-registration is not available for this league");
    }
}
