package com.klasio.shared.infrastructure.exception;

public class SessionCancelledException extends RuntimeException {

    public SessionCancelledException(String message) {
        super(message);
    }
}
