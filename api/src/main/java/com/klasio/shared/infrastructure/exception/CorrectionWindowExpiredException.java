package com.klasio.shared.infrastructure.exception;

public class CorrectionWindowExpiredException extends RuntimeException {
    public CorrectionWindowExpiredException(String message) {
        super(message);
    }
}
