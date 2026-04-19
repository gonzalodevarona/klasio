package com.klasio.shared.infrastructure.exception;

public class InsufficientHoursException extends RuntimeException {

    public InsufficientHoursException(String message) {
        super(message);
    }
}
