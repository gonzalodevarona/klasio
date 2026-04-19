package com.klasio.shared.infrastructure.exception;

public class SessionFullException extends RuntimeException {

    public SessionFullException(String message) {
        super(message);
    }
}
