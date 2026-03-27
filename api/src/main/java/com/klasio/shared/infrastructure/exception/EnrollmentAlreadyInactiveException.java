package com.klasio.shared.infrastructure.exception;

public class EnrollmentAlreadyInactiveException extends RuntimeException {

    public EnrollmentAlreadyInactiveException(String message) {
        super(message);
    }
}
