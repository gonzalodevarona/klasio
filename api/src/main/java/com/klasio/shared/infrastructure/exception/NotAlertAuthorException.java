package com.klasio.shared.infrastructure.exception;

public class NotAlertAuthorException extends RuntimeException {
    public NotAlertAuthorException() {
        super("Only the original alert author can update the reason");
    }
}
