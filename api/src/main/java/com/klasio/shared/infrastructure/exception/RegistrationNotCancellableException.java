package com.klasio.shared.infrastructure.exception;

public class RegistrationNotCancellableException extends RuntimeException {
    public RegistrationNotCancellableException(String currentStatus) {
        super("Registration cannot be cancelled in its current status: " + currentStatus);
    }
}
