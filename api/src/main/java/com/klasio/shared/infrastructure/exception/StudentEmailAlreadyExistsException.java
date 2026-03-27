package com.klasio.shared.infrastructure.exception;

public class StudentEmailAlreadyExistsException extends RuntimeException {

    public StudentEmailAlreadyExistsException(String message) {
        super(message);
    }
}
