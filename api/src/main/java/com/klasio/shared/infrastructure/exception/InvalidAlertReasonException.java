package com.klasio.shared.infrastructure.exception;

public class InvalidAlertReasonException extends RuntimeException {
    public InvalidAlertReasonException(String message) {
        super(message);
    }
}
