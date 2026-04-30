package com.klasio.shared.infrastructure.exception;

public class AlreadyMarkedException extends RuntimeException {
    public AlreadyMarkedException(String message) {
        super(message);
    }
}
