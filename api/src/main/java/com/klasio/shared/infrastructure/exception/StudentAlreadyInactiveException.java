package com.klasio.shared.infrastructure.exception;

public class StudentAlreadyInactiveException extends RuntimeException {

    public StudentAlreadyInactiveException(String message) {
        super(message);
    }
}
