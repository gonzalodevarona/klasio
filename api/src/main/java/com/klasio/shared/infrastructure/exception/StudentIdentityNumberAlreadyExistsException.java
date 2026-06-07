package com.klasio.shared.infrastructure.exception;

public class StudentIdentityNumberAlreadyExistsException extends RuntimeException {
    public StudentIdentityNumberAlreadyExistsException(String message) {
        super(message);
    }
}
