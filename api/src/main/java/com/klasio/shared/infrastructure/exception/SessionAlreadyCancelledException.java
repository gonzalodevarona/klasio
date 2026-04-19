package com.klasio.shared.infrastructure.exception;

public class SessionAlreadyCancelledException extends RuntimeException {
    public SessionAlreadyCancelledException() {
        super("Session is already cancelled");
    }
}
