package com.klasio.shared.infrastructure.exception;

public class StudentAlreadyActiveException extends RuntimeException {

    public StudentAlreadyActiveException(String message) {
        super(message);
    }
}
